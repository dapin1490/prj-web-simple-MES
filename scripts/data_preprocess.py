from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List

try:
    import pandas as pd
except ImportError as import_error:
    raise SystemExit(
        "필수 패키지 pandas가 없습니다. `python -m pip install pandas openpyxl` 후 다시 실행하세요."
    ) from import_error

PASS_FAIL_THRESHOLD = 1.0
CSV_ENCODINGS = ("utf-8", "cp949", "euc-kr")
INPUT_FILE_NAMES = {
    "customer_order": "MES_customer_order.csv",
    "production_trend": "PRODUCTION_TREND.csv",
    "lot_amount": "LOT_amount_preprocessed.xlsx",
    "ccm_measure": "CCM_measure_preprocessed.xlsx",
}
OUTPUT_FILE_NAMES = {
    "products": "Products.csv",
    "sales_orders": "SalesOrders.csv",
    "work_orders": "WorkOrders.csv",
    "production_logs": "ProductionLogs.csv",
    "inspections": "Inspections.csv",
}


@dataclass
class InputFrames:
    customer_order: pd.DataFrame
    production_trend: pd.DataFrame
    lot_amount: pd.DataFrame
    ccm_measure: pd.DataFrame


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="원본 데이터를 백엔드 적재용 CSV로 전처리합니다."
    )
    parser.add_argument(
        "--input-dir",
        type=Path,
        default=Path("data"),
        help="원본 데이터 디렉터리 경로",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path("data/backend_ready"),
        help="결과 CSV 출력 디렉터리 경로",
    )
    return parser.parse_args()


def read_csv_with_fallback_encoding(file_path: Path) -> pd.DataFrame:
    latest_exception: Exception | None = None
    for encoding_name in CSV_ENCODINGS:
        try:
            return pd.read_csv(file_path, encoding=encoding_name)
        except Exception as error:
            latest_exception = error
    raise RuntimeError(f"CSV 파일을 읽지 못했습니다: {file_path}") from latest_exception


def ensure_required_columns(
    frame_name: str, data_frame: pd.DataFrame, required_columns: Iterable[str]
) -> None:
    missing_columns = [column_name for column_name in required_columns if column_name not in data_frame.columns]
    if missing_columns:
        missing_text = ", ".join(missing_columns)
        raise ValueError(f"{frame_name} 필수 컬럼 누락: {missing_text}")


def to_float_series(raw_series: pd.Series) -> pd.Series:
    normalized_series = (
        raw_series.astype(str)
        .str.replace(",", "", regex=False)
        .str.replace(" ", "", regex=False)
        .replace({"": pd.NA, "nan": pd.NA, "None": pd.NA})
    )
    return pd.to_numeric(normalized_series, errors="coerce")


def to_int_series(raw_series: pd.Series) -> pd.Series:
    return to_float_series(raw_series).fillna(0).round().astype(int)


def load_input_frames(input_directory: Path) -> InputFrames:
    customer_order_path = input_directory / INPUT_FILE_NAMES["customer_order"]
    production_trend_path = input_directory / INPUT_FILE_NAMES["production_trend"]
    lot_amount_path = input_directory / INPUT_FILE_NAMES["lot_amount"]
    ccm_measure_path = input_directory / INPUT_FILE_NAMES["ccm_measure"]

    for input_file_path in (
        customer_order_path,
        production_trend_path,
        lot_amount_path,
        ccm_measure_path,
    ):
        if not input_file_path.exists():
            raise FileNotFoundError(f"입력 파일이 없습니다: {input_file_path}")

    customer_order_frame = read_csv_with_fallback_encoding(customer_order_path)
    production_trend_frame = read_csv_with_fallback_encoding(production_trend_path)
    lot_amount_frame = pd.read_excel(lot_amount_path)
    ccm_measure_frame = pd.read_excel(ccm_measure_path)

    ensure_required_columns(
        "MES_customer_order.csv",
        customer_order_frame,
        ["제품계층구조", "안전재고"],
    )
    ensure_required_columns(
        "PRODUCTION_TREND.csv",
        production_trend_frame,
        ["LOT_NO", "RESOURCE_CD", "INSRT_DT", "TRD_TEMP_PV", "TRD_SPEED1"],
    )
    ensure_required_columns(
        "LOT_amount_preprocessed.xlsx",
        lot_amount_frame,
        ["LOT번호", "공정코드", "염색길이(m)"],
    )
    ensure_required_columns(
        "CCM_measure_preprocessed.xlsx",
        ccm_measure_frame,
        ["LOT번호", "검사차수", "염색색차 DE"],
    )

    return InputFrames(
        customer_order=customer_order_frame,
        production_trend=production_trend_frame,
        lot_amount=lot_amount_frame,
        ccm_measure=ccm_measure_frame,
    )


def build_reference_frames(
    input_frames: InputFrames,
) -> tuple[pd.DataFrame, pd.DataFrame]:
    production_trend_frame = input_frames.production_trend.copy()
    production_trend_frame["timestamp"] = pd.to_datetime(
        production_trend_frame["INSRT_DT"], errors="coerce"
    )
    production_trend_frame = production_trend_frame.dropna(subset=["timestamp"])
    production_trend_frame["wo_id"] = production_trend_frame["LOT_NO"].astype(str).str.strip()
    production_trend_frame["machine_id"] = (
        production_trend_frame["RESOURCE_CD"].astype(str).str.strip()
    )
    production_trend_frame["temp_pv"] = to_float_series(production_trend_frame["TRD_TEMP_PV"])
    production_trend_frame["speed"] = to_int_series(production_trend_frame["TRD_SPEED1"])

    lot_amount_frame = input_frames.lot_amount.copy()
    lot_amount_frame["wo_id"] = lot_amount_frame["LOT번호"].astype(str).str.strip()
    lot_amount_frame["product_id"] = lot_amount_frame["공정코드"].astype(str).str.strip()
    lot_amount_frame["planned_qty"] = to_float_series(lot_amount_frame["염색길이(m)"]).fillna(0.0)
    lot_amount_frame = lot_amount_frame[["wo_id", "product_id", "planned_qty"]].drop_duplicates("wo_id")

    first_log_date_by_lot = (
        production_trend_frame.groupby("wo_id", as_index=False)["timestamp"]
        .min()
        .rename(columns={"timestamp": "first_timestamp"})
    )
    first_log_date_by_lot["order_date"] = first_log_date_by_lot["first_timestamp"].dt.date

    work_base_frame = lot_amount_frame.merge(first_log_date_by_lot, on="wo_id", how="left")
    work_base_frame = work_base_frame.dropna(subset=["order_date"])

    return production_trend_frame, work_base_frame


def create_products_frame(
    input_frames: InputFrames, work_base_frame: pd.DataFrame
) -> pd.DataFrame:
    product_ids = sorted(work_base_frame["product_id"].dropna().unique().tolist())
    if not product_ids:
        raise ValueError("product_id를 생성할 수 없습니다.")

    default_category_series = to_float_series(input_frames.customer_order["제품계층구조"]).dropna()
    default_safety_stock_series = to_float_series(input_frames.customer_order["안전재고"]).dropna()

    default_category = (
        str(int(default_category_series.iloc[0])) if not default_category_series.empty else ""
    )
    default_safety_stock = (
        int(round(float(default_safety_stock_series.iloc[0])))
        if not default_safety_stock_series.empty
        else 0
    )

    product_rows: List[Dict[str, object]] = []
    for product_id in product_ids:
        product_rows.append(
            {
                "product_id": product_id,
                "name": product_id,
                "category": default_category,
                "safety_stock": default_safety_stock,
            }
        )

    return pd.DataFrame(product_rows)[
        ["product_id", "name", "category", "safety_stock"]
    ]


def create_sales_orders_frame(work_base_frame: pd.DataFrame) -> pd.DataFrame:
    daily_totals = (
        work_base_frame.groupby("order_date", as_index=False)["planned_qty"]
        .sum()
        .rename(columns={"planned_qty": "order_qty"})
        .sort_values("order_date")
        .reset_index(drop=True)
    )
    if daily_totals.empty:
        raise ValueError("SalesOrders를 생성할 데이터가 없습니다.")

    daily_totals["order_id"] = daily_totals["order_date"].apply(
        lambda date_value: f"SO{date_value.strftime('%Y%m%d')}"
    )
    daily_totals["product_id"] = (
        work_base_frame.groupby("order_date")["product_id"].first().reindex(daily_totals["order_date"]).values
    )

    return daily_totals[["order_id", "product_id", "order_date", "order_qty"]]


def create_work_orders_frame(
    work_base_frame: pd.DataFrame, sales_orders_frame: pd.DataFrame, production_trend_frame: pd.DataFrame
) -> pd.DataFrame:
    order_id_by_date = {
        row.order_date: row.order_id for row in sales_orders_frame.itertuples(index=False)
    }

    machine_by_lot = (
        production_trend_frame.groupby("wo_id", as_index=False)["machine_id"]
        .agg(lambda series: series.mode().iat[0] if not series.mode().empty else series.iloc[0])
    )

    work_orders_frame = work_base_frame.copy()
    work_orders_frame["order_id"] = work_orders_frame["order_date"].map(order_id_by_date)
    work_orders_frame = work_orders_frame.merge(machine_by_lot, on="wo_id", how="left")
    work_orders_frame["machine_id"] = work_orders_frame["machine_id"].fillna("")

    return work_orders_frame[["wo_id", "order_id", "planned_qty", "machine_id"]].sort_values("wo_id")


def create_production_logs_frame(production_trend_frame: pd.DataFrame) -> pd.DataFrame:
    production_logs_frame = production_trend_frame[
        ["wo_id", "timestamp", "temp_pv", "speed"]
    ].copy()
    production_logs_frame = production_logs_frame.sort_values(["timestamp", "wo_id"]).reset_index(
        drop=True
    )
    production_logs_frame["log_id"] = production_logs_frame.index + 1

    return production_logs_frame[["log_id", "wo_id", "timestamp", "temp_pv", "speed"]]


def create_inspections_frame(input_frames: InputFrames) -> pd.DataFrame:
    inspections_source = input_frames.ccm_measure.copy()
    inspections_source["wo_id"] = inspections_source["LOT번호"].astype(str).str.strip()
    inspections_source["inspection_step"] = to_float_series(inspections_source["검사차수"]).fillna(0)
    inspections_source["color_de"] = to_float_series(inspections_source["염색색차 DE"])
    inspections_source = inspections_source.dropna(subset=["wo_id", "color_de"])
    inspections_source = inspections_source.sort_values(["wo_id", "inspection_step"])
    latest_inspection_by_lot = inspections_source.groupby("wo_id", as_index=False).tail(1).copy()
    latest_inspection_by_lot = latest_inspection_by_lot.sort_values("wo_id").reset_index(drop=True)

    latest_inspection_by_lot["insp_id"] = latest_inspection_by_lot.index.map(
        lambda index_value: f"INSP{index_value + 1:07d}"
    )
    latest_inspection_by_lot["pass_fail"] = (
        latest_inspection_by_lot["color_de"] < PASS_FAIL_THRESHOLD
    )

    return latest_inspection_by_lot[["insp_id", "wo_id", "color_de", "pass_fail"]]


def run_validation_checks(
    sales_orders_frame: pd.DataFrame,
    work_orders_frame: pd.DataFrame,
    production_logs_frame: pd.DataFrame,
) -> None:
    unknown_work_order_ids = set(production_logs_frame["wo_id"]) - set(work_orders_frame["wo_id"])
    if unknown_work_order_ids:
        raise ValueError(
            f"ProductionLogs에 WorkOrders에 없는 wo_id가 있습니다. 건수: {len(unknown_work_order_ids)}"
        )

    order_quantity_by_order_id = (
        work_orders_frame.groupby("order_id", as_index=False)["planned_qty"].sum()
        .rename(columns={"planned_qty": "work_sum"})
    )
    compare_frame = sales_orders_frame.merge(order_quantity_by_order_id, on="order_id", how="left")
    compare_frame["work_sum"] = compare_frame["work_sum"].fillna(0.0)
    quantity_mismatch_frame = compare_frame[
        (compare_frame["order_qty"] - compare_frame["work_sum"]).abs() > 1e-6
    ]
    if not quantity_mismatch_frame.empty:
        raise ValueError("SalesOrders.order_qty와 WorkOrders.planned_qty 합계가 일치하지 않습니다.")

    earliest_timestamp_by_order = (
        work_orders_frame.merge(
            production_logs_frame[["wo_id", "timestamp"]], on="wo_id", how="left"
        )
        .groupby("order_id", as_index=False)["timestamp"]
        .min()
    )
    date_check_frame = sales_orders_frame.merge(earliest_timestamp_by_order, on="order_id", how="left")
    invalid_date_frame = date_check_frame[
        pd.to_datetime(date_check_frame["order_date"], errors="coerce")
        > pd.to_datetime(date_check_frame["timestamp"], errors="coerce")
    ]
    if not invalid_date_frame.empty:
        raise ValueError("SalesOrders.order_date가 ProductionLogs.timestamp보다 늦은 데이터가 있습니다.")


def write_output_frames(
    output_directory: Path,
    products_frame: pd.DataFrame,
    sales_orders_frame: pd.DataFrame,
    work_orders_frame: pd.DataFrame,
    production_logs_frame: pd.DataFrame,
    inspections_frame: pd.DataFrame,
) -> None:
    output_directory.mkdir(parents=True, exist_ok=True)

    products_frame.to_csv(
        output_directory / OUTPUT_FILE_NAMES["products"], index=False, encoding="utf-8"
    )
    sales_orders_export = sales_orders_frame.copy()
    sales_orders_export["order_date"] = pd.to_datetime(
        sales_orders_export["order_date"]
    ).dt.strftime("%Y-%m-%d")
    sales_orders_export.to_csv(
        output_directory / OUTPUT_FILE_NAMES["sales_orders"], index=False, encoding="utf-8"
    )

    work_orders_frame.to_csv(
        output_directory / OUTPUT_FILE_NAMES["work_orders"], index=False, encoding="utf-8"
    )

    production_logs_export = production_logs_frame.copy()
    production_logs_export["timestamp"] = pd.to_datetime(
        production_logs_export["timestamp"]
    ).dt.strftime("%Y-%m-%d %H:%M:%S")
    production_logs_export.to_csv(
        output_directory / OUTPUT_FILE_NAMES["production_logs"], index=False, encoding="utf-8"
    )

    inspections_frame.to_csv(
        output_directory / OUTPUT_FILE_NAMES["inspections"], index=False, encoding="utf-8"
    )


def main() -> None:
    arguments = parse_arguments()
    input_frames = load_input_frames(arguments.input_dir)
    production_trend_frame, work_base_frame = build_reference_frames(input_frames)

    products_frame = create_products_frame(input_frames, work_base_frame)
    sales_orders_frame = create_sales_orders_frame(work_base_frame)
    work_orders_frame = create_work_orders_frame(
        work_base_frame, sales_orders_frame, production_trend_frame
    )
    production_logs_frame = create_production_logs_frame(production_trend_frame)
    inspections_frame = create_inspections_frame(input_frames)

    run_validation_checks(sales_orders_frame, work_orders_frame, production_logs_frame)
    write_output_frames(
        arguments.output_dir,
        products_frame,
        sales_orders_frame,
        work_orders_frame,
        production_logs_frame,
        inspections_frame,
    )

    print(f"생성 완료: {arguments.output_dir}")
    for output_name in OUTPUT_FILE_NAMES.values():
        print(f"- {arguments.output_dir / output_name}")


if __name__ == "__main__":
    main()
