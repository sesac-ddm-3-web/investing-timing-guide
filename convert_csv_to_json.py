#!/usr/bin/env python3
"""
CSV 파일을 JSON 형태로 변환하는 스크립트
history 폴더의 CSV 파일을 읽어서 src/main/resources/data에 JSON으로 저장합니다.
"""

import csv
import json
import os
from datetime import datetime

HISTORY_DIR = "history"
DATA_DIR = "src/main/resources/data"
TICKERS = ["QQQ", "VOO", "SOXX"]


def parse_volume(volume_str):
    """
    거래량 문자열을 숫자로 변환
    예: "80.09M" -> 80090000
    """
    volume_str = volume_str.strip().upper()

    if 'M' in volume_str:
        # M (백만)
        number = float(volume_str.replace('M', ''))
        return int(number * 1_000_000)
    elif 'K' in volume_str:
        # K (천)
        number = float(volume_str.replace('K', ''))
        return int(number * 1000)
    elif 'B' in volume_str:
        # B (십억)
        number = float(volume_str.replace('B', ''))
        return int(number * 1_000_000_000)
    else:
        # 그냥 숫자
        return int(float(volume_str.replace(',', '')))


def parse_price(price_str):
    """
    가격 문자열을 float로 변환
    """
    return float(price_str.strip().replace(',', ''))


def parse_date(date_str):
    """
    날짜 문자열을 YYYY-MM-DD 형식으로 변환
    입력: "2025- 11- 14" (공백 포함)
    출력: "2025-11-14"
    """
    # 공백 제거
    date_str = date_str.strip().replace(' ', '')
    # YYYY-MM-DD 형식으로 파싱
    date_obj = datetime.strptime(date_str, "%Y-%m-%d")
    return date_obj.strftime("%Y-%m-%d")


def convert_csv_to_json(ticker):
    """
    특정 티커의 CSV 파일을 JSON으로 변환
    """
    csv_file = os.path.join(HISTORY_DIR, f"{ticker}_historical_data.csv")

    if not os.path.exists(csv_file):
        print(f"❌ CSV 파일을 찾을 수 없습니다: {csv_file}")
        return None

    print(f"📖 Reading {csv_file}...")

    stock_data = []

    with open(csv_file, 'r', encoding='utf-8-sig') as f:  # BOM 제거를 위해 utf-8-sig 사용
        reader = csv.reader(f)

        # 헤더 읽기 및 정리
        headers = next(reader)
        headers = [h.strip().strip('"') for h in headers]  # 공백 및 따옴표 제거

        print(f"  CSV headers: {headers}")

        for row in reader:
            try:
                # CSV 컬럼 인덱스: 0=날짜, 1=종가, 2=시가, 3=고가, 4=저가, 5=거래량, 6=변동 %
                date = parse_date(row[0])
                close = parse_price(row[1])
                open_price = parse_price(row[2])
                high = parse_price(row[3])
                low = parse_price(row[4])
                volume = parse_volume(row[5])

                stock_data.append({
                    "date": date,
                    "open": round(open_price, 2),
                    "high": round(high, 2),
                    "low": round(low, 2),
                    "close": round(close, 2),
                    "volume": volume
                })
            except Exception as e:
                print(f"⚠️  Error parsing row for {ticker}: {row}")
                print(f"   Error: {e}")
                continue

    # 날짜 순으로 정렬 (오래된 날짜가 먼저)
    stock_data.sort(key=lambda x: x['date'])

    print(f"✓ Parsed {len(stock_data)} records for {ticker}")
    print(f"  Date range: {stock_data[0]['date']} to {stock_data[-1]['date']}")

    return stock_data


def save_json(ticker, data):
    """
    JSON 파일로 저장
    """
    # 데이터 디렉토리 생성
    if not os.path.exists(DATA_DIR):
        os.makedirs(DATA_DIR)
        print(f"Created directory: {DATA_DIR}")

    json_file = os.path.join(DATA_DIR, f"{ticker}.json")

    with open(json_file, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

    print(f"✓ Saved {ticker} data to {json_file}")


def main():
    """
    메인 함수
    """
    print("=" * 60)
    print("CSV to JSON Converter")
    print("=" * 60)
    print(f"Converting CSV files from {HISTORY_DIR}/ to {DATA_DIR}/")
    print("=" * 60)
    print()

    for ticker in TICKERS:
        print(f"Processing {ticker}...")
        data = convert_csv_to_json(ticker)

        if data:
            save_json(ticker, data)
            print()
        else:
            print(f"❌ Failed to convert {ticker}")
            print()

    print("=" * 60)
    print("Conversion complete!")
    print("=" * 60)
    print(f"\nJSON files saved in: {DATA_DIR}/")
    print("\nYou can now start the Spring Boot application:")
    print("  ./mvnw spring-boot:run")
    print("\nThen open: http://localhost:8080")


if __name__ == "__main__":
    main()
