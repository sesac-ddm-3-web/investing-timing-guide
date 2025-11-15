# 투자 타이밍 가이드 (Stock Investment Guide)

주요 ETF들의 고점 대비 하락률을 분석하고, 과거 유사한 하락 시점에서의 수익률을 제공하여 투자 타이밍을 가이드하는 웹 애플리케이션입니다.

**📌 중요: 로컬 CSV 데이터 사용**
이 애플리케이션은 `history/` 폴더의 CSV 파일을 사용합니다. Yahoo Finance API는 사용하지 않습니다.

## 주요 기능

### 1. 현재 하락률 분석
- QQQ, VOO, SOXX 등 주요 ETF의 최고점 대비 현재 하락률 표시
- 최고가 도달 시점 및 경과 일수 제공
- 실시간 가격 정보 표시

### 2. 과거 사례 분석
- 현재와 유사한 하락률(±2%)을 보였던 과거 사례 검색
- 각 사례별 하락 시작일과 저점 도달일 제공
- 저점에서 투자 시 1개월, 3개월, 6개월, 12개월, 24개월 후 수익률 계산

### 3. 데이터 관리
- 로컬 CSV 파일에서 데이터 로드 (`history/` 폴더)
- CSV를 JSON 형태로 변환하여 저장
- 수동으로 CSV 업데이트 및 재변환 가능
- 약 15년치 과거 데이터 포함 (QQQ: 3,994개, VOO: 3,820개, SOXX: 3,994개 거래일)

### 4. 직관적인 웹 UI
- 반응형 디자인으로 모바일/데스크톱 지원
- 실시간 분석 결과 시각화
- 여러 ETF 동시 비교 기능

## 기술 스택

- **Backend**: Spring Boot 3.5.7, Java 21
- **Frontend**: HTML5, CSS3, Vanilla JavaScript
- **Data Source**: 로컬 CSV 파일 (`history/` 폴더)
- **Storage**: JSON 파일 (로컬 파일 시스템)
- **Build Tool**: Maven
- **지원 ETF**: QQQ, VOO, SOXX

## 프로젝트 구조

```
demo/
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/
│   │   │   ├── controller/      # REST API 컨트롤러
│   │   │   │   └── StockController.java
│   │   │   ├── service/          # 비즈니스 로직
│   │   │   │   ├── StockDataService.java
│   │   │   │   ├── AnalysisService.java
│   │   │   │   └── DataUpdateScheduler.java
│   │   │   ├── repository/       # 데이터 저장/로드
│   │   │   │   └── JsonDataRepository.java
│   │   │   ├── model/            # 데이터 모델
│   │   │   │   ├── StockData.java
│   │   │   │   ├── DrawdownAnalysis.java
│   │   │   │   ├── HistoricalDrawdown.java
│   │   │   │   └── RecoveryPeriod.java
│   │   │   ├── dto/              # API 응답 DTO
│   │   │   │   └── StockAnalysisResponse.java
│   │   │   ├── config/           # 설정
│   │   │   │   └── SchedulerConfig.java
│   │   │   └── DemoApplication.java
│   │   └── resources/
│   │       ├── static/           # 프론트엔드 파일
│   │       │   ├── index.html
│   │       │   ├── css/style.css
│   │       │   └── js/app.js
│   │       ├── data/             # JSON 데이터 저장 위치
│   │       └── application.properties
│   └── test/
└── pom.xml
```

## 설치 및 실행

### 사전 요구사항
- Java 21 이상
- Maven (포함됨: mvnw)
- Python 3 (CSV 변환용)

### 실행 방법

#### 1. 프로젝트 디렉토리로 이동
```bash
cd /Users/foo/Downloads/demo
```

#### 2. **[중요] CSV 데이터를 JSON으로 변환** (최초 1회 필수)

이미 `history/` 폴더에 CSV 파일이 있는 경우:
```bash
python3 convert_csv_to_json.py
```

이 스크립트는:
- `history/QQQ_historical_data.csv` → `src/main/resources/data/QQQ.json`
- `history/VOO_historical_data.csv` → `src/main/resources/data/VOO.json`
- `history/SOXX_historical_data.csv` → `src/main/resources/data/SOXX.json`

**CSV 파일 형식:**
```csv
"날짜","종가","시가","고가","저가","거래량","변동 %"
"2025- 11- 14","608.86","599.55","613.35","597.17","80.09M","0.08%"
```

#### 3. 애플리케이션 실행
```bash
./mvnw spring-boot:run
```

#### 4. 웹 브라우저에서 접속
```
http://localhost:8080
```

### 데이터 업데이트 방법

1. `history/` 폴더의 CSV 파일 업데이트
2. CSV를 JSON으로 재변환:
   ```bash
   python3 convert_csv_to_json.py
   ```
3. 애플리케이션 재시작 (자동으로 새 데이터 로드)

### 빌드 방법

```bash
# 컴파일만
./mvnw clean compile

# JAR 파일 생성
./mvnw clean package

# JAR 파일 실행
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

## API 엔드포인트

### 1. 건강 체크
```
GET /api/stocks/health
```

### 2. 특정 ETF 분석
```
GET /api/stocks/{ticker}/analysis?years=10

예시: GET /api/stocks/QQQM/analysis?years=10
```

### 3. 데이터 새로고침 (비활성화됨)
```
POST /api/stocks/{ticker}/refresh

로컬 CSV 데이터를 사용하므로 API를 통한 새로고침은 지원하지 않습니다.
대신 CSV 파일을 업데이트하고 convert_csv_to_json.py를 실행하세요.
```

### 4. 전체 ETF 분석
```
GET /api/stocks/analysis/all?years=2
```

### 5. 지원 티커 목록
```
GET /api/stocks/supported

응답: ["QQQ", "VOO", "SOXX"]
```

### 6. 초기 데이터 수집 (비활성화됨)
```
POST /api/stocks/initialize

로컬 CSV 데이터를 사용하므로 API를 통한 초기화는 지원하지 않습니다.
convert_csv_to_json.py 스크립트를 사용하세요.
```

## 사용 방법

### 웹 UI 사용

1. 브라우저에서 `http://localhost:8080` 접속
2. 상단 드롭다운에서 분석할 ETF 선택 (QQQ, VOO, SOXX)
3. "분석하기" 버튼 클릭
4. 결과 확인:
   - 현재 상태: 현재가, 최고가, 하락률 등
   - 과거 유사 사례: 비슷한 하락률을 보인 과거 사례와 그때의 수익률

### 전체 ETF 동시 분석

"전체 분석" 버튼을 클릭하면 모든 ETF를 한 번에 분석하여 비교할 수 있습니다.

### 데이터 새로고침

"데이터 새로고침" 버튼을 클릭하면 Yahoo Finance에서 최신 데이터를 다시 받아옵니다.

## 데이터 저장 위치

주식 데이터는 다음 위치에 JSON 형태로 저장됩니다:
```
src/main/resources/data/{TICKER}.json
```

예시:
- `src/main/resources/data/QQQ.json`
- `src/main/resources/data/VOO.json`
- `src/main/resources/data/SOXX.json`

## 데이터 업데이트

**자동 업데이트 스케줄러는 비활성화되었습니다.**

로컬 CSV 파일을 사용하므로 자동 업데이트가 필요하지 않습니다.

### 데이터 업데이트 방법

1. **CSV 파일 업데이트**
   - `history/` 폴더의 CSV 파일을 수동으로 업데이트합니다
   - 또는 외부 소스에서 새 CSV 파일을 받아 덮어씁니다

2. **JSON 재변환**
   ```bash
   python3 convert_csv_to_json.py
   ```

3. **애플리케이션 재시작** (옵션)
   - 실행 중인 애플리케이션을 재시작하면 새 데이터가 자동으로 로드됩니다
   - 또는 데이터가 필요할 때 자동으로 다시 로드됩니다

## 주의사항

### 데이터 소스

- **로컬 CSV 파일 사용**: Yahoo Finance API는 사용하지 않습니다
- **데이터 신뢰성**: CSV 파일의 정확성과 최신성은 사용자가 관리해야 합니다
- **지원 티커**: QQQ, VOO, SOXX만 지원됩니다

### 데이터 정확성

- 데이터는 `history/` 폴더의 CSV 파일에 의존합니다
- 종가 기준 데이터입니다
- 배당금이나 수수료는 고려되지 않습니다
- 현재 보유한 데이터: 약 15년치 (2010년 ~ 2025년)

## 커스터마이징

### 다른 ETF 추가

1. `StockController.java`의 `DEFAULT_TICKERS` 수정:
```java
private static final List<String> DEFAULT_TICKERS =
    Arrays.asList("QQQ", "VOO", "SOXX", "SPY", "VTI");
```

2. `index.html`의 select 옵션 추가:
```html
<option value="SPY">SPY (S&P 500)</option>
```

### CSV 파일 추가

새로운 티커를 추가하려면:

1. `history/` 폴더에 새 CSV 파일 추가 (예: `history/SPY_historical_data.csv`)
2. `convert_csv_to_json.py`의 `TICKERS` 목록에 추가
3. `StockController.java`의 `DEFAULT_TICKERS`에 추가
4. `index.html`의 select 옵션에 추가
5. `python3 convert_csv_to_json.py` 실행

### 분석 범위 조정

`AnalysisService.java`의 tolerance 값 조정:
```java
// 현재: ±2% 범위
BigDecimal tolerance = BigDecimal.valueOf(2.0);

// 더 넓은 범위로 변경 (예: ±5%)
BigDecimal tolerance = BigDecimal.valueOf(5.0);
```

## 향후 개선사항

### 1. Telegram 알림 기능 (예정)
- 특정 하락률 도달 시 알림
- 투자 적기 알림
- 일일 리포트 전송

### 2. 추가 기능 아이디어
- 더 많은 ETF 지원
- 차트 시각화
- 이메일 알림
- 포트폴리오 추적
- 백테스팅 기능

## 문제 해결

### 애플리케이션이 시작되지 않는 경우
```bash
# 포트 8080이 사용 중인지 확인
lsof -i :8080

# 다른 포트로 실행
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### 데이터가 로드되지 않는 경우
1. 인터넷 연결 확인
2. Yahoo Finance 접근 가능 여부 확인
3. 로그 확인: `target/logs/` 또는 콘솔 출력

### 컴파일 에러
```bash
# 의존성 재설치
./mvnw clean install

# 캐시 삭제 후 재컴파일
rm -rf target/
./mvnw clean compile
```

## 라이선스

이 프로젝트는 개인 프로젝트이며, 교육 및 개인 사용 목적으로 제작되었습니다.

## 면책 조항

이 애플리케이션은 투자 참고 목적으로만 사용해야 하며, 실제 투자 결정은 본인의 판단과 책임 하에 이루어져야 합니다. 과거 수익률이 미래 수익을 보장하지 않습니다.
