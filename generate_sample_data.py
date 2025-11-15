#!/usr/bin/env python3
"""
샘플 데이터 생성기
실제 Yahoo Finance 데이터 대신 현실적인 샘플 데이터를 생성합니다.
테스트 및 데모 목적으로 사용하세요.
"""

import json
import os
from datetime import datetime, timedelta
import random
import math

DATA_DIR = "src/main/resources/data"

def generate_realistic_stock_data(ticker, start_price, volatility, trend, years=2):
    """
    현실적인 주식 데이터 생성

    Args:
        ticker: 티커 심볼
        start_price: 시작 가격
        volatility: 변동성 (0.01 = 1% 일일 변동)
        trend: 추세 (양수 = 상승, 음수 = 하락)
        years: 생성할 년수
    """
    print(f"Generating sample data for {ticker}...")

    stock_data = []
    current_date = datetime.now() - timedelta(days=years * 365)
    end_date = datetime.now()
    current_price = start_price

    trading_days = 0

    while current_date <= end_date:
        # 주말 제외
        if current_date.weekday() < 5:
            # 일일 변동 계산 (랜덤 워크 + 추세)
            daily_return = random.gauss(trend/252, volatility)  # 연간 기준을 일일로 변환
            current_price = current_price * (1 + daily_return)

            # 장중 변동 시뮬레이션
            daily_volatility = current_price * volatility * random.uniform(0.5, 1.5)

            open_price = current_price * (1 + random.gauss(0, volatility/2))
            high_price = max(open_price, current_price) + abs(random.gauss(0, daily_volatility))
            low_price = min(open_price, current_price) - abs(random.gauss(0, daily_volatility))
            close_price = current_price

            # 거래량 생성 (로그 정규 분포)
            base_volume = 5000000
            volume = int(base_volume * math.exp(random.gauss(0, 0.5)))

            stock_data.append({
                "date": current_date.strftime("%Y-%m-%d"),
                "open": round(open_price, 2),
                "high": round(high_price, 2),
                "low": round(low_price, 2),
                "close": round(close_price, 2),
                "volume": volume
            })

            trading_days += 1

            # 가끔 큰 조정 발생 (시장 이벤트 시뮬레이션)
            if random.random() < 0.005:  # 0.5% 확률
                correction = random.uniform(-0.15, -0.05)  # 5-15% 하락
                current_price = current_price * (1 + correction)
                print(f"  Market correction on {current_date.strftime('%Y-%m-%d')}: {correction*100:.1f}%")

        current_date += timedelta(days=1)

    print(f"✓ Generated {len(stock_data)} trading days for {ticker}")
    return stock_data

def save_sample_data():
    """샘플 데이터 생성 및 저장"""

    # 데이터 디렉토리 생성
    if not os.path.exists(DATA_DIR):
        os.makedirs(DATA_DIR)
        print(f"Created directory: {DATA_DIR}")

    print("=" * 60)
    print("Sample Stock Data Generator")
    print("=" * 60)
    print("Generating realistic sample data for testing...")
    print("=" * 60)
    print()

    # QQQ: Nasdaq-100 추종 (높은 변동성, 강한 상승 추세)
    qqq_data = generate_realistic_stock_data(
        ticker="QQQ",
        start_price=400.0,
        volatility=0.015,  # 1.5% 일일 변동성
        trend=0.15,  # 15% 연간 상승 추세
        years=2
    )

    print()

    # VOO: S&P 500 추종 (중간 변동성, 안정적 상승)
    voo_data = generate_realistic_stock_data(
        ticker="VOO",
        start_price=400.0,
        volatility=0.012,  # 1.2% 일일 변동성
        trend=0.10,  # 10% 연간 상승 추세
        years=2
    )

    print()

    # SOXX: 반도체 인덱스 (높은 변동성, 성장주)
    soxx_data = generate_realistic_stock_data(
        ticker="SOXX",
        start_price=400.0,
        volatility=0.018,  # 1.8% 일일 변동성
        trend=0.18,  # 18% 연간 상승 추세
        years=2
    )

    print()

    # JSON 파일로 저장
    for ticker, data in [("QQQ", qqq_data), ("VOO", voo_data), ("SOXX", soxx_data)]:
        file_path = os.path.join(DATA_DIR, f"{ticker}.json")
        with open(file_path, 'w') as f:
            json.dump(data, f, indent=2)
        print(f"✓ Saved {ticker} data to {file_path}")

    print()
    print("=" * 60)
    print("Sample data generation complete!")
    print("=" * 60)
    print(f"\nData saved in: {DATA_DIR}/")
    print("\nYou can now start the Spring Boot application:")
    print("  ./mvnw spring-boot:run")
    print("\nThen open: http://localhost:8080")
    print()
    print("NOTE: This is sample data for testing.")
    print("To get real data, wait 5-10 minutes and run:")
    print("  python3 collect_stock_data.py")

if __name__ == "__main__":
    save_sample_data()
