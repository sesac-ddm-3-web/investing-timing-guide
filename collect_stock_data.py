#!/usr/bin/env python3
"""
Stock Data Collector
주식 데이터를 Yahoo Finance에서 수집하여 JSON으로 저장합니다.
서버 실행 여부와 무관하게 독립적으로 실행 가능합니다.

의존성: 표준 라이브러리만 사용 (추가 설치 불필요)
"""

import json
import os
import urllib.request
import urllib.error
from datetime import datetime, timedelta
import time
import sys
import csv
from io import StringIO
import ssl

# 설정
TICKERS = ["QQQ", "VOO", "SOXX"]
YEARS_BACK = 2  # 2년치 데이터면 충분
DATA_DIR = "src/main/resources/data"
DELAY_SECONDS = 15  # Alpha Vantage 무료 API는 분당 5회 제한 (12초 간격 권장)

# Alpha Vantage API Key (무료 발급: https://www.alphavantage.co/support/#api-key)
# 환경변수로 설정하거나 여기에 직접 입력
ALPHA_VANTAGE_API_KEY = os.environ.get('ALPHA_VANTAGE_API_KEY', 'demo')  # 'demo'는 제한적

def ensure_data_dir():
    """데이터 디렉토리가 없으면 생성"""
    if not os.path.exists(DATA_DIR):
        os.makedirs(DATA_DIR)
        print(f"✓ Created directory: {DATA_DIR}")

def fetch_stock_data(ticker, years_back=2):
    """
    Alpha Vantage API에서 주식 데이터 수집

    Args:
        ticker: 주식 티커 심볼 (예: "QQQM")
        years_back: 과거 몇 년치 데이터를 가져올지

    Returns:
        list: 주식 데이터 리스트 (날짜순 정렬)
    """
    try:
        print(f"\n📊 Fetching data for {ticker}...")

        if ALPHA_VANTAGE_API_KEY == 'demo':
            print(f"⚠️  Using demo API key (limited data)")
            print(f"   Get free API key at: https://www.alphavantage.co/support/#api-key")

        # Alpha Vantage API URL - TIME_SERIES_DAILY_ADJUSTED (최대 20년 데이터)
        url = f"https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol={ticker}&apikey={ALPHA_VANTAGE_API_KEY}&outputsize=full"

        # 데이터 다운로드
        headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36'
        }

        req = urllib.request.Request(url, headers=headers)
        ssl_context = ssl._create_unverified_context()
        response = urllib.request.urlopen(req, timeout=30, context=ssl_context)
        data = json.loads(response.read().decode('utf-8'))

        # API 에러 체크
        if 'Error Message' in data:
            print(f"❌ API Error: {data['Error Message']}")
            return []

        if 'Note' in data:
            print(f"❌ API Rate Limit: {data['Note']}")
            print(f"   Alpha Vantage 무료 API는 분당 5회, 일일 100회 제한이 있습니다.")
            return []

        if 'Time Series (Daily)' not in data:
            print(f"❌ No time series data found for {ticker}")
            return []

        # JSON 파싱
        time_series = data['Time Series (Daily)']
        stock_data = []

        # 날짜 필터링 (years_back만큼)
        cutoff_date = datetime.now() - timedelta(days=years_back * 365)

        for date_str, values in time_series.items():
            try:
                date_obj = datetime.strptime(date_str, '%Y-%m-%d')

                # 지정된 기간 내의 데이터만 포함
                if date_obj < cutoff_date:
                    continue

                stock_data.append({
                    "date": date_str,
                    "open": float(values['1. open']),
                    "high": float(values['2. high']),
                    "low": float(values['3. low']),
                    "close": float(values['4. close']),
                    "volume": int(values['6. volume'])
                })
            except (ValueError, KeyError) as e:
                # 잘못된 데이터는 건너뛰기
                continue

        if not stock_data:
            print(f"⚠️  No data found for {ticker}")
            return []

        # 날짜순 정렬
        stock_data.sort(key=lambda x: x['date'])

        print(f"✓ Successfully fetched {len(stock_data)} records for {ticker}")
        return stock_data

    except urllib.error.HTTPError as e:
        print(f"❌ HTTP Error {e.code} for {ticker}: {e.reason}")
        return []
    except Exception as e:
        print(f"❌ Error fetching data for {ticker}: {str(e)}")
        return []

def save_to_json(ticker, data):
    """
    데이터를 JSON 파일로 저장

    Args:
        ticker: 주식 티커 심볼
        data: 저장할 데이터 리스트
    """
    try:
        file_path = os.path.join(DATA_DIR, f"{ticker}.json")

        with open(file_path, 'w') as f:
            json.dump(data, f, indent=2)

        print(f"✓ Saved to {file_path}")

    except Exception as e:
        print(f"❌ Error saving data for {ticker}: {str(e)}")

def collect_all_data():
    """모든 티커의 데이터 수집"""
    print("=" * 60)
    print("Stock Data Collector (Alpha Vantage)")
    print("=" * 60)
    print(f"Tickers: {', '.join(TICKERS)}")
    print(f"Period: Last {YEARS_BACK} years")
    print(f"Delay between requests: {DELAY_SECONDS} seconds")
    print(f"API Key: {ALPHA_VANTAGE_API_KEY[:4]}..." if ALPHA_VANTAGE_API_KEY != 'demo' else "API Key: demo (limited)")
    print("=" * 60)

    # 데이터 디렉토리 확인/생성
    ensure_data_dir()

    success_count = 0
    failed_count = 0

    for i, ticker in enumerate(TICKERS):
        # Rate limiting 방지를 위한 대기 (첫 번째 요청은 제외)
        if i > 0:
            print(f"\n⏳ Waiting {DELAY_SECONDS} seconds to avoid rate limiting...")
            time.sleep(DELAY_SECONDS)

        # 데이터 수집
        data = fetch_stock_data(ticker, YEARS_BACK)

        if data:
            save_to_json(ticker, data)
            success_count += 1
        else:
            failed_count += 1

    # 결과 요약
    print("\n" + "=" * 60)
    print("Collection Complete!")
    print("=" * 60)
    print(f"✓ Success: {success_count} tickers")
    print(f"❌ Failed: {failed_count} tickers")

    if success_count > 0:
        print(f"\nData saved in: {DATA_DIR}/")
        print("\nYou can now start the Spring Boot application and use the data!")

    return success_count, failed_count

def update_recent_data(ticker, days_back=7):
    """
    최근 데이터만 업데이트 (증분 업데이트)

    Args:
        ticker: 주식 티커 심볼
        days_back: 최근 며칠치 데이터를 가져올지
    """
    try:
        print(f"\n📊 Updating recent data for {ticker} (last {days_back} days)...")

        # 기존 데이터 로드
        file_path = os.path.join(DATA_DIR, f"{ticker}.json")
        existing_data = []

        if os.path.exists(file_path):
            with open(file_path, 'r') as f:
                existing_data = json.load(f)
            print(f"✓ Loaded {len(existing_data)} existing records")

        # 최근 데이터 수집
        end_date = datetime.now()
        start_date = end_date - timedelta(days=days_back)

        period1 = int(start_date.timestamp())
        period2 = int(end_date.timestamp())

        url = f"https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol={ticker}&apikey={ALPHA_VANTAGE_API_KEY}&outputsize=compact"

        headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36'
        }

        req = urllib.request.Request(url, headers=headers)
        ssl_context = ssl._create_unverified_context()
        response = urllib.request.urlopen(req, timeout=30, context=ssl_context)
        data = json.loads(response.read().decode('utf-8'))

        if 'Time Series (Daily)' not in data:
            print(f"⚠️  No recent data found for {ticker}")
            return

        # JSON 파싱
        time_series = data['Time Series (Daily)']
        new_records = []

        # 날짜 필터링
        cutoff_date = datetime.now() - timedelta(days=days_back)

        for date_str, values in time_series.items():
            try:
                date_obj = datetime.strptime(date_str, '%Y-%m-%d')

                if date_obj < cutoff_date:
                    continue

                new_records.append({
                    "date": date_str,
                    "open": float(values['1. open']),
                    "high": float(values['2. high']),
                    "low": float(values['3. low']),
                    "close": float(values['4. close']),
                    "volume": int(values['6. volume'])
                })
            except (ValueError, KeyError):
                continue

        if not new_records:
            print(f"⚠️  No recent data found for {ticker}")
            return

        # 기존 데이터와 병합 (중복 제거)
        existing_dates = {record['date'] for record in existing_data}
        new_count = 0

        for record in new_records:
            if record['date'] not in existing_dates:
                existing_data.append(record)
                new_count += 1

        # 날짜순 정렬
        existing_data.sort(key=lambda x: x['date'])

        # 저장
        with open(file_path, 'w') as f:
            json.dump(existing_data, f, indent=2)

        print(f"✓ Added {new_count} new records (total: {len(existing_data)})")

    except Exception as e:
        print(f"❌ Error updating data for {ticker}: {str(e)}")

def update_all_recent():
    """모든 티커의 최근 데이터 업데이트"""
    print("=" * 60)
    print("Update Recent Data (Last 7 Days)")
    print("=" * 60)

    for i, ticker in enumerate(TICKERS):
        if i > 0:
            print(f"\n⏳ Waiting {DELAY_SECONDS} seconds...")
            time.sleep(DELAY_SECONDS)

        update_recent_data(ticker, days_back=7)

    print("\n" + "=" * 60)
    print("Update Complete!")
    print("=" * 60)

if __name__ == "__main__":
    # 명령행 인자 확인
    if len(sys.argv) > 1 and sys.argv[1] == "--update":
        # 최근 데이터만 업데이트
        update_all_recent()
    else:
        # 전체 데이터 수집
        collect_all_data()
