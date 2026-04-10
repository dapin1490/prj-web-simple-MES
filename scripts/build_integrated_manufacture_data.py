from __future__ import annotations

from pathlib import Path
from typing import Iterable

try:
    import pandas as pd
except ImportError as import_error:
    raise SystemExit(
        "필수 패키지 pandas가 없습니다. `python -m pip install pandas openpyxl` 후 다시 실행하세요."
    ) from import_error


DATA_DIR = Path("data")
PRODUCTION_TREND_FILE = DATA_DIR / "PRODUCTION_TREND.csv"
LOT_AMOUNT_FILE = DATA_DIR / "LOT_amount.xlsx"
CCM_MEASURE_FILE = DATA_DIR / "CCM_measure.xlsx"
OUTPUT_FILE = DATA_DIR / "integrated_manufacture_data.csv"

CSV_ENCODINGS = ("utf-8", "cp949", "euc-kr")


def read_csv_with_fallback_encoding(file_path: Path, use_columns: Iterable[str]) -> pd.DataFrame:
    last_exception: Exception | None = None
    for encoding_name in CSV_ENCODINGS:
        try:
            return pd.read_csv(
                file_path,
                encoding=encoding_name,
                usecols=list(use_columns),
                low_memory=False,
            )
        except Exception as error:
            last_exception = error
    raise RuntimeError(f"CSV 파일을 읽지 못했습니다: {file_path}") from last_exception


def to_numeric_series(raw_series: pd.Series) -> pd.Series:
    normalized_series = (
        raw_series.astype(str)
        .str.replace(",", "", regex=False)
        .str.replace(" ", "", regex=False)
        .replace({"": pd.NA, "nan": pd.NA, "None": pd.NA})
    )
    return pd.to_numeric(normalized_series, errors="coerce")


def prepare_lot_amount_frame(lot_amount_frame: pd.DataFrame) -> pd.DataFrame:
    required_columns = [
        "PRODT_ORDER_NO",
        "EXT1_QTY(투입중량 (KG))",
        "EXT2_QTY (액량 (LITER))",
        "염색 가동 길이",
    ]
    missing_columns = [column_name for column_name in required_columns if column_name not in lot_amount_frame.columns]
    if missing_columns:
        raise ValueError(f"LOT_amount.xlsx 필수 컬럼 누락: {', '.join(missing_columns)}")

    prepared_frame = lot_amount_frame[required_columns].copy()
    prepared_frame = prepared_frame.rename(
        columns={
            "PRODT_ORDER_NO": "LOT_NO",
            "EXT1_QTY(투입중량 (KG))": "EXT1_QTY",
            "EXT2_QTY (액량 (LITER))": "EXT2_QTY",
            "염색 가동 길이": "염색 가동 길이",
        }
    )
    prepared_frame["LOT_NO"] = prepared_frame["LOT_NO"].astype(str).str.strip()
    prepared_frame["EXT1_QTY"] = to_numeric_series(prepared_frame["EXT1_QTY"])
    prepared_frame["EXT2_QTY"] = to_numeric_series(prepared_frame["EXT2_QTY"])
    prepared_frame["염색 가동 길이"] = to_numeric_series(prepared_frame["염색 가동 길이"])
    prepared_frame = prepared_frame.drop_duplicates(subset=["LOT_NO"], keep="first")
    return prepared_frame


def prepare_ccm_measure_frame(ccm_measure_frame: pd.DataFrame) -> pd.DataFrame:
    required_columns = ["lot_no", "seq", "염색 색차 DE"]
    missing_columns = [column_name for column_name in required_columns if column_name not in ccm_measure_frame.columns]
    if missing_columns:
        raise ValueError(f"CCM_measure.xlsx 필수 컬럼 누락: {', '.join(missing_columns)}")

    prepared_frame = ccm_measure_frame[required_columns].copy()
    prepared_frame = prepared_frame.rename(
        columns={
            "lot_no": "LOT_NO",
            "seq": "quality_seq",
            "염색 색차 DE": "염색 색차 DE",
        }
    )
    prepared_frame["LOT_NO"] = prepared_frame["LOT_NO"].astype(str).str.strip()
    prepared_frame["quality_seq"] = to_numeric_series(prepared_frame["quality_seq"]).fillna(-1)
    prepared_frame["염색 색차 DE"] = to_numeric_series(prepared_frame["염색 색차 DE"])

    # lot 단위 최종 품질값 1건만 남김 (검사차수 최대값 우선)
    prepared_frame = prepared_frame.sort_values(["LOT_NO", "quality_seq"]).drop_duplicates(
        subset=["LOT_NO"], keep="last"
    )
    return prepared_frame[["LOT_NO", "염색 색차 DE"]]


def main() -> None:
    for input_file_path in [PRODUCTION_TREND_FILE, LOT_AMOUNT_FILE, CCM_MEASURE_FILE]:
        if not input_file_path.exists():
            raise FileNotFoundError(f"입력 파일이 없습니다: {input_file_path}")

    production_trend_columns = [
        "LOT_NO",
        "WC_CD",
        "WC_CNT",
        "SEQ_NO",
        "PGM_ID",
        "RESOURCE_CD",
        "CR_TEMP",
        "TRD_TEMP_SP",
        "TRD_TEMP_PV",
        "TRD_SPEED1",
        "TRD_SPEED2",
        "TRD_SPEED3",
        "TRD_SPEED4",
        "INSRT_DT",
        "PRODUCTION_RESULT_iD",
    ]

    production_trend_frame = read_csv_with_fallback_encoding(
        PRODUCTION_TREND_FILE, production_trend_columns
    )
    lot_amount_frame = pd.read_excel(LOT_AMOUNT_FILE)
    ccm_measure_frame = pd.read_excel(CCM_MEASURE_FILE)

    production_trend_frame["LOT_NO"] = production_trend_frame["LOT_NO"].astype(str).str.strip()
    production_trend_frame["SEQ_NO"] = to_numeric_series(production_trend_frame["SEQ_NO"])
    production_trend_frame["TRD_TEMP_PV"] = to_numeric_series(production_trend_frame["TRD_TEMP_PV"])

    lot_amount_prepared_frame = prepare_lot_amount_frame(lot_amount_frame)
    ccm_measure_prepared_frame = prepare_ccm_measure_frame(ccm_measure_frame)

    integrated_frame = production_trend_frame.merge(
        lot_amount_prepared_frame,
        on="LOT_NO",
        how="left",
    )
    integrated_frame = integrated_frame.merge(
        ccm_measure_prepared_frame,
        on="LOT_NO",
        how="left",
    )

    max_seq_series = integrated_frame.groupby("LOT_NO")["SEQ_NO"].transform("max")
    integrated_frame["공정진행시간(%)"] = (integrated_frame["SEQ_NO"] / max_seq_series) * 100.0

    integrated_frame["투입중량/길이"] = integrated_frame["EXT1_QTY"] / integrated_frame["염색 가동 길이"]
    integrated_frame["투입중량/액량"] = integrated_frame["EXT1_QTY"] / integrated_frame["EXT2_QTY"]

    missing_length_by_lot = integrated_frame["염색 가동 길이"].isna().groupby(integrated_frame["LOT_NO"]).any()
    missing_temp_by_lot = integrated_frame["TRD_TEMP_PV"].isna().groupby(integrated_frame["LOT_NO"]).any()
    valid_lot_mask = ~(missing_length_by_lot | missing_temp_by_lot)
    valid_lot_set = set(valid_lot_mask[valid_lot_mask].index)
    integrated_frame = integrated_frame[integrated_frame["LOT_NO"].isin(valid_lot_set)].copy()

    integrated_frame = integrated_frame.sort_values(["LOT_NO", "SEQ_NO"], kind="stable").reset_index(drop=True)
    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    integrated_frame.to_csv(OUTPUT_FILE, index=False, encoding="utf-8")

    print("통합 파일 생성 완료")
    print(f"- 출력 파일: {OUTPUT_FILE}")
    print(f"- 결과 행 수: {len(integrated_frame):,}")
    print(f"- 결과 LOT 수: {integrated_frame['LOT_NO'].nunique():,}")


if __name__ == "__main__":
    main()
