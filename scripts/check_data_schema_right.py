from __future__ import annotations

import argparse
import sys
from pathlib import Path
from typing import Callable, List, Tuple

try:
    import pandas as pd
except ImportError as import_error:
    raise SystemExit(
        "필수 패키지 pandas가 없습니다. `python -m pip install pandas` 후 다시 실행하세요."
    ) from import_error

CSV_ENCODINGS = ("utf-8", "cp949", "euc-kr")

BACKEND_FILES = {
    "products": "Products.csv",
    "sales_orders": "SalesOrders.csv",
    "work_orders": "WorkOrders.csv",
    "production_logs": "ProductionLogs.csv",
    "inspections": "Inspections.csv",
}

LogicalType = str

TABLE_SCHEMA: List[Tuple[str, List[Tuple[str, LogicalType]]]] = [
    (
        BACKEND_FILES["products"],
        [
            ("product_id", "string"),
            ("name", "string"),
            ("category", "string"),
            ("safety_stock", "int"),
        ],
    ),
    (
        BACKEND_FILES["sales_orders"],
        [
            ("order_id", "string"),
            ("product_id", "string"),
            ("order_date", "date"),
            ("order_qty", "float"),
        ],
    ),
    (
        BACKEND_FILES["work_orders"],
        [
            ("wo_id", "string"),
            ("order_id", "string"),
            ("planned_qty", "float"),
            ("machine_id", "string"),
        ],
    ),
    (
        BACKEND_FILES["production_logs"],
        [
            ("log_id", "int"),
            ("wo_id", "string"),
            ("timestamp", "datetime"),
            ("cr_temp", "int"),
            ("temp_sp", "float"),
            ("temp_pv", "float"),
            ("speed", "int"),
        ],
    ),
    (
        BACKEND_FILES["inspections"],
        [
            ("insp_id", "string"),
            ("wo_id", "string"),
            ("color_de", "float"),
            ("pass_fail", "bool"),
        ],
    ),
]


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "data/backend_ready CSV가 docs/data-schema-definition.md §2 컬럼명·타입과 "
            "일치하는지 검사합니다."
        )
    )
    parser.add_argument(
        "--data-dir",
        type=Path,
        default=Path("data/backend_ready"),
        help="백엔드 적재용 CSV 디렉터리 경로",
    )
    return parser.parse_args()


def read_csv_with_fallback_encoding(file_path: Path) -> pd.DataFrame:
    latest_exception: Exception | None = None
    for encoding_name in CSV_ENCODINGS:
        try:
            return pd.read_csv(file_path, encoding=encoding_name, low_memory=False)
        except Exception as error:
            latest_exception = error
    raise RuntimeError(f"CSV 파일을 읽지 못했습니다: {file_path}") from latest_exception


def _is_string_like_dtype(series: pd.Series) -> bool:
    if pd.api.types.is_object_dtype(series) or pd.api.types.is_string_dtype(series):
        return True
    if pd.api.types.is_integer_dtype(series):
        return True
    return False


def _validate_string_column(column_name: str, series: pd.Series) -> str | None:
    if _is_string_like_dtype(series):
        return None
    if pd.api.types.is_float_dtype(series) and series.dropna().apply(float.is_integer).all():
        return None
    return f"{column_name}: VARCHAR에 맞지 않는 dtype ({series.dtype})"


def _validate_int_column(column_name: str, series: pd.Series) -> str | None:
    if pd.api.types.is_integer_dtype(series):
        return None
    if pd.api.types.is_float_dtype(series) and series.dropna().apply(lambda value: float(value).is_integer()).all():
        return None
    return f"{column_name}: INT/BIGINT에 맞지 않는 dtype 또는 비정수 값 ({series.dtype})"


def _validate_float_column(column_name: str, series: pd.Series) -> str | None:
    if pd.api.types.is_float_dtype(series) or pd.api.types.is_integer_dtype(series):
        return None
    return f"{column_name}: FLOAT에 맞지 않는 dtype ({series.dtype})"


def _validate_bool_column(column_name: str, series: pd.Series) -> str | None:
    if pd.api.types.is_bool_dtype(series):
        return None
    if pd.api.types.is_object_dtype(series):
        allowed = {True, False, "True", "False", "true", "false"}
        bad = series.dropna().map(lambda value: value not in allowed)
        if not bad.any():
            return None
    return f"{column_name}: BOOLEAN에 맞지 않는 값 또는 dtype ({series.dtype})"


def _validate_date_column(column_name: str, series: pd.Series) -> str | None:
    parsed = pd.to_datetime(series, errors="coerce")
    if series.notna().any() and parsed.isna().all():
        return f"{column_name}: DATE로 해석할 수 없는 값"
    return None


def _validate_datetime_column(column_name: str, series: pd.Series) -> str | None:
    if pd.api.types.is_datetime64_any_dtype(series):
        return None
    parsed = pd.to_datetime(series, errors="coerce")
    if series.astype(str).str.strip().ne("").any() and parsed.isna().all():
        return f"{column_name}: DATETIME으로 해석할 수 없는 값"
    return None


def validate_column_logical_type(
    column_name: str, series: pd.Series, logical_type: LogicalType
) -> str | None:
    validators: dict[str, Callable[[str, pd.Series], str | None]] = {
        "string": _validate_string_column,
        "int": _validate_int_column,
        "float": _validate_float_column,
        "bool": _validate_bool_column,
        "date": _validate_date_column,
        "datetime": _validate_datetime_column,
    }
    validator = validators.get(logical_type)
    if validator is None:
        return f"{column_name}: 알 수 없는 논리 타입 {logical_type}"
    return validator(column_name, series)


def check_table_schema(
    data_directory: Path, file_name: str, column_specs: List[Tuple[str, LogicalType]]
) -> List[str]:
    errors: List[str] = []
    file_path = data_directory / file_name
    if not file_path.exists():
        return [f"{file_name}: 파일이 없습니다 ({file_path})"]

    try:
        data_frame = read_csv_with_fallback_encoding(file_path)
    except RuntimeError as read_error:
        return [f"{file_name}: {read_error}"]

    expected_columns = [column_name for column_name, _ in column_specs]
    actual_columns = list(data_frame.columns)
    if actual_columns != expected_columns:
        errors.append(
            f"{file_name}: 컬럼 순서 또는 이름 불일치\n"
            f"  기대: {expected_columns}\n"
            f"  실제: {actual_columns}"
        )
        return errors

    for column_name, logical_type in column_specs:
        column_series = data_frame[column_name]
        type_error = validate_column_logical_type(column_name, column_series, logical_type)
        if type_error is not None:
            errors.append(f"{file_name}: {type_error}")

    return errors


def main() -> None:
    arguments = parse_arguments()
    data_directory: Path = arguments.data_dir

    all_errors: List[str] = []
    for file_name, column_specs in TABLE_SCHEMA:
        all_errors.extend(check_table_schema(data_directory, file_name, column_specs))

    if all_errors:
        print("스키마 검사 실패 (docs/data-schema-definition.md §2 기준)", file=sys.stderr)
        for message in all_errors:
            print(f"- {message}", file=sys.stderr)
        raise SystemExit(1)

    print("스키마 검사 통과: 모든 백엔드 CSV가 정의서 컬럼명·순서·타입과 일치합니다.")
    print(f"- 검사 경로: {data_directory.resolve()}")


if __name__ == "__main__":
    main()
