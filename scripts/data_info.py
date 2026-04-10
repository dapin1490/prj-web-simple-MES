from io import StringIO
from pathlib import Path

import pandas as pd


DATA_DIRECTORY_PATH = Path("data")
OUTPUT_TEXT_FILE_PATH = Path("docs/data_info_report.txt")
SUPPORTED_FILE_EXTENSIONS = (".csv", ".xlsx")
SECTION_SEPARATOR = "=" * 100
SUBSECTION_SEPARATOR = "-" * 100


def dataframe_info_as_text(target_dataframe: pd.DataFrame) -> str:
    info_buffer = StringIO()
    target_dataframe.info(buf=info_buffer)
    return info_buffer.getvalue()


def dataframe_describe_as_text(target_dataframe: pd.DataFrame) -> str:
    try:
        describe_dataframe = target_dataframe.describe(include="all")
        return describe_dataframe.to_string()
    except ValueError:
        return "describe 결과를 생성할 수 없습니다. (데이터가 비어 있거나 요약 가능한 컬럼이 없습니다.)"


def build_report_for_dataframe(
    source_file_path: Path,
    target_dataframe: pd.DataFrame,
    sheet_name: str | None = None,
) -> str:
    header_lines = [
        SECTION_SEPARATOR,
        f"파일: {source_file_path}",
    ]

    if sheet_name is not None:
        header_lines.append(f"시트: {sheet_name}")

    header_lines.extend(
        [
            SUBSECTION_SEPARATOR,
            "[INFO]",
            dataframe_info_as_text(target_dataframe),
            SUBSECTION_SEPARATOR,
            "[DESCRIBE]",
            dataframe_describe_as_text(target_dataframe),
            "",
        ]
    )

    return "\n".join(header_lines)


def collect_report_sections(data_directory_path: Path) -> list[str]:
    report_sections: list[str] = []

    target_file_paths = sorted(
        [
            file_path
            for file_path in data_directory_path.rglob("*")
            if file_path.is_file() and file_path.suffix.lower() in SUPPORTED_FILE_EXTENSIONS
        ]
    )

    for target_file_path in target_file_paths:
        file_extension = target_file_path.suffix.lower()

        if file_extension == ".csv":
            csv_dataframe = pd.read_csv(target_file_path, encoding="cp949")
            report_sections.append(build_report_for_dataframe(target_file_path, csv_dataframe))

        elif file_extension == ".xlsx":
            excel_sheet_dataframes = pd.read_excel(target_file_path, sheet_name=None)
            for sheet_name, sheet_dataframe in excel_sheet_dataframes.items():
                report_sections.append(
                    build_report_for_dataframe(
                        source_file_path=target_file_path,
                        target_dataframe=sheet_dataframe,
                        sheet_name=str(sheet_name),
                    )
                )

    return report_sections


def main() -> None:
    if not DATA_DIRECTORY_PATH.exists():
        raise FileNotFoundError(f"`{DATA_DIRECTORY_PATH}` 폴더를 찾을 수 없습니다.")

    print(f"데이터 정보 수집 시작: {DATA_DIRECTORY_PATH.resolve()}")
    report_sections = collect_report_sections(DATA_DIRECTORY_PATH)

    if not report_sections:
        report_text = (
            f"`{DATA_DIRECTORY_PATH}` 폴더에서 처리할 파일을 찾지 못했습니다. "
            f"(지원 확장자: {', '.join(SUPPORTED_FILE_EXTENSIONS)})\n"
        )
    else:
        report_text = "\n".join(report_sections)

    OUTPUT_TEXT_FILE_PATH.write_text(report_text, encoding="utf-8")
    print(f"저장 완료: {OUTPUT_TEXT_FILE_PATH.resolve()}")


if __name__ == "__main__":
    main()
