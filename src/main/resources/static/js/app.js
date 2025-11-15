const API_BASE_URL = '/api/stocks';

// DOM Elements
const tickerSelect = document.getElementById('ticker-select');
const loading = document.getElementById('loading');
const errorMessage = document.getElementById('error-message');
const results = document.getElementById('results');

// Store current data for navigation
let currentAnalysisData = null;
let currentTicker = null;

// Event Listeners
tickerSelect.addEventListener('change', () => {
    const ticker = tickerSelect.value;
    // Update URL and analyze
    const url = new URL(window.location);
    url.searchParams.set('ticker', ticker);
    url.searchParams.delete('level'); // Remove level param when changing ticker
    window.history.pushState({ ticker }, '', url);
    analyzeStock(ticker);
});

// Handle browser back/forward buttons
window.addEventListener('popstate', (event) => {
    const urlParams = new URLSearchParams(window.location.search);
    const ticker = urlParams.get('ticker') || 'QQQ';
    const level = urlParams.get('level');

    // Update ticker selector
    if (tickerSelect.value !== ticker) {
        tickerSelect.value = ticker;
        analyzeStock(ticker);
    } else if (level && currentAnalysisData) {
        // Show detail view for the level
        const levelData = currentAnalysisData.drawdownLevelAnalyses?.find(
            l => l.drawdownLevel === parseInt(level)
        );
        if (levelData) {
            showDetailView(levelData, ticker, false); // false = don't push state
        }
    } else {
        // Show main view
        hideDetailView(false); // false = don't push state
    }
});

// Load from URL on page load
window.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const ticker = urlParams.get('ticker') || tickerSelect.value;
    const level = urlParams.get('level');

    // Set ticker selector
    tickerSelect.value = ticker;

    // Set initial URL if not present
    if (!urlParams.get('ticker')) {
        const url = new URL(window.location);
        url.searchParams.set('ticker', ticker);
        window.history.replaceState({ ticker }, '', url);
    }

    // Analyze stock
    analyzeStock(ticker).then(() => {
        // If level param exists, show detail view
        if (level && currentAnalysisData) {
            const levelData = currentAnalysisData.drawdownLevelAnalyses?.find(
                l => l.drawdownLevel === parseInt(level)
            );
            if (levelData) {
                setTimeout(() => {
                    showDetailView(levelData, ticker, false); // false = don't modify history
                }, 100);
            }
        }
    });
});

// Show/Hide UI elements
function showLoading() {
    loading.style.display = 'block';
    errorMessage.style.display = 'none';
    results.innerHTML = '';
}

function hideLoading() {
    loading.style.display = 'none';
}

function showError(message) {
    errorMessage.textContent = message;
    errorMessage.style.display = 'block';
    hideLoading();
}

// API Calls
async function analyzeStock(ticker) {
    showLoading();
    try {
        const response = await fetch(`${API_BASE_URL}/${ticker}/analysis?years=10`);
        if (!response.ok) {
            throw new Error('데이터를 가져오는데 실패했습니다');
        }
        const data = await response.json();

        // Store data for navigation
        currentAnalysisData = data;
        currentTicker = ticker;

        hideLoading();
        displayStockAnalysis(ticker, data);
        return data;
    } catch (error) {
        showError(error.message);
        throw error;
    }
}


// Display Functions
function displayStockAnalysis(ticker, data) {
    results.innerHTML = '';

    if (!data.currentDrawdown) {
        results.innerHTML = '<div class="no-data">데이터를 찾을 수 없습니다</div>';
        return;
    }

    // Display data period info
    if (data.dataStartDate && data.dataEndDate) {
        const periodInfo = document.createElement('div');
        periodInfo.className = 'data-period-info';
        periodInfo.innerHTML = `
            <span class="period-label">분석 데이터 기간:</span>
            <span class="period-dates">${data.dataStartDate} ~ ${data.dataEndDate}</span>
        `;
        results.appendChild(periodInfo);
    }

    const card = createStockCard(ticker, data);
    results.appendChild(card);
}


function createStockCard(ticker, data) {
    const card = document.createElement('div');
    card.className = 'stock-card';

    const drawdown = data.currentDrawdown;
    const historicals = data.historicalDrawdowns || [];
    const drawdownLevels = data.drawdownLevelAnalyses || [];
    const chartData = data.oneYearChartData;

    const drawdownClass = drawdown.drawdownPercent >= 0 ? 'positive' : 'negative';

    card.innerHTML = `
        <div class="stock-header">
            <h2 class="stock-title">${ticker}</h2>
        </div>

        <div class="drawdown-info">
            <h3>현재 상태</h3>
            ${chartData ? `
                <div class="chart-container">
                    <canvas id="chart-${ticker}" width="400" height="200"></canvas>
                </div>
            ` : ''}
            <div class="info-grid">
                <div class="info-item">
                    <span class="info-label">현재가</span>
                    <span class="info-value">$${formatNumber(drawdown.currentPrice)}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">최고가</span>
                    <span class="info-value">$${formatNumber(drawdown.peakPrice)}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">최고가 도달일</span>
                    <span class="info-value">${drawdown.peakDate}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">고점 대비 하락률</span>
                    <span class="info-value drawdown-percent ${drawdownClass}">
                        ${formatPercent(drawdown.drawdownPercent)}
                    </span>
                </div>
                <div class="info-item">
                    <span class="info-label">고점 이후 경과일</span>
                    <span class="info-value">${drawdown.daysSincePeak}일</span>
                </div>
            </div>
        </div>

        ${createHistoricalSection(historicals, drawdown.drawdownPercent, ticker)}
        ${createDrawdownLevelsSection(drawdownLevels, ticker)}
    `;

    // Draw chart after card is added to DOM
    if (chartData) {
        setTimeout(() => {
            createPriceChart(ticker, chartData, drawdown);
        }, 100);
    }

    return card;
}

function createHistoricalSection(historicals, currentDrawdown, ticker) {
    if (!historicals || historicals.length === 0) {
        return `
            <div class="historical-section">
                <h3>과거 유사 하락 사례</h3>
                <div class="no-data">
                    현재 하락률(${formatPercent(currentDrawdown)})과 유사한 과거 사례를 찾을 수 없습니다.
                </div>
            </div>
        `;
    }

    const historicalItems = historicals.map((hist, index) => {
        const chartId = `hist-chart-${ticker}-${index}`;
        return `
            <div class="historical-item">
                <div class="historical-header">
                    <div class="historical-date">
                        <strong>하락 시작:</strong> ${hist.startDate} →
                        <strong>저점:</strong> ${hist.bottomDate}
                    </div>
                    <div class="info-value ${hist.drawdownPercent >= 0 ? 'positive' : 'negative'}">
                        하락률: ${formatPercent(hist.drawdownPercent)}
                    </div>
                </div>
                ${hist.chartData ? `
                    <div class="chart-container">
                        <canvas id="${chartId}" width="400" height="150"></canvas>
                    </div>
                ` : ''}
                <div>
                    <strong>해당 저점에서 투자 시 수익률:</strong>
                    <div class="recovery-grid">
                        ${hist.recoveryPeriods.map(period => `
                            <div class="recovery-item">
                                <div class="recovery-months">${period.months}개월 후</div>
                                <div class="recovery-return ${period.returnPercent >= 0 ? 'positive' : 'negative'}">
                                    ${formatPercent(period.returnPercent)}
                                </div>
                            </div>
                        `).join('')}
                    </div>
                </div>
            </div>
        `;
    }).join('');

    // Draw charts after rendering
    setTimeout(() => {
        historicals.forEach((hist, index) => {
            if (hist.chartData) {
                const chartId = `hist-chart-${ticker}-${index}`;
                createHistoricalDrawdownChart(chartId, hist);
            }
        });
    }, 100);

    return `
        <div class="historical-section">
            <h3>과거 유사 하락 사례 (±2% 범위)</h3>
            <p style="color: #718096; margin-bottom: 15px;">
                현재와 비슷한 하락률을 보였던 ${historicals.length}건의 과거 사례를 분석했습니다.
                각 차트는 고점 3개월 전부터 저점 12개월 후까지의 가격 변화를 보여줍니다.
            </p>
            <div class="historical-list">
                ${historicalItems}
            </div>
        </div>
    `;
}

function createDrawdownLevelsSection(drawdownLevels, ticker) {
    if (!drawdownLevels || drawdownLevels.length === 0) {
        return '';
    }

    const levelItems = drawdownLevels.map((level, index) => {
        if (!level.averageStats || level.totalCases === 0) {
            return `
                <div class="level-item" style="cursor: default; pointer-events: none;">
                    <div class="level-header">
                        <h4>${level.drawdownLevel}% 하락 시</h4>
                        <span class="case-count">과거 사례: 0건</span>
                    </div>
                    <div class="no-data">과거 데이터 없음</div>
                </div>
            `;
        }

        const stats = level.averageStats;

        return `
            <div class="level-item" data-level-index="${index}">
                <div class="level-header">
                    <h4>${level.drawdownLevel}% 하락 시</h4>
                    <span class="case-count">과거 사례: ${level.totalCases}건</span>
                </div>
                <div class="level-stats">
                    <p style="color: #718096; margin-bottom: 10px; font-size: 14px;">
                        해당 하락률에서 투자 시 평균 수익률
                    </p>
                    <div class="recovery-grid">
                        <div class="recovery-item">
                            <div class="recovery-months">1개월 후</div>
                            <div class="recovery-return ${stats.month1Avg >= 0 ? 'positive' : 'negative'}">
                                ${formatPercent(stats.month1Avg)}
                            </div>
                            <div class="loss-count">손실: ${stats.month1LossCount}/${level.totalCases}건</div>
                        </div>
                        <div class="recovery-item">
                            <div class="recovery-months">3개월 후</div>
                            <div class="recovery-return ${stats.month3Avg >= 0 ? 'positive' : 'negative'}">
                                ${formatPercent(stats.month3Avg)}
                            </div>
                            <div class="loss-count">손실: ${stats.month3LossCount}/${level.totalCases}건</div>
                        </div>
                        <div class="recovery-item">
                            <div class="recovery-months">6개월 후</div>
                            <div class="recovery-return ${stats.month6Avg >= 0 ? 'positive' : 'negative'}">
                                ${formatPercent(stats.month6Avg)}
                            </div>
                            <div class="loss-count">손실: ${stats.month6LossCount}/${level.totalCases}건</div>
                        </div>
                        <div class="recovery-item">
                            <div class="recovery-months">12개월 후</div>
                            <div class="recovery-return ${stats.month12Avg >= 0 ? 'positive' : 'negative'}">
                                ${formatPercent(stats.month12Avg)}
                            </div>
                            <div class="loss-count">손실: ${stats.month12LossCount}/${level.totalCases}건</div>
                        </div>
                        <div class="recovery-item">
                            <div class="recovery-months">24개월 후</div>
                            <div class="recovery-return ${stats.month24Avg >= 0 ? 'positive' : 'negative'}">
                                ${formatPercent(stats.month24Avg)}
                            </div>
                            <div class="loss-count">손실: ${stats.month24LossCount}/${level.totalCases}건</div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }).join('');

    // Add event listeners after rendering
    setTimeout(() => {
        attachLevelClickListeners(drawdownLevels, ticker);
    }, 100);

    return `
        <div class="drawdown-levels-section">
            <h3>시나리오별 분석 - 고정 하락률 기준</h3>
            <p style="color: #718096; margin-bottom: 20px;">
                만약 고점 대비 특정 하락률을 기록한다면? 과거 동일한 하락률에서의 평균 수익률을 확인하세요.
            </p>
            <div class="levels-grid">
                ${levelItems}
            </div>
        </div>
    `;
}

function attachLevelClickListeners(drawdownLevels, ticker) {
    const levelItems = document.querySelectorAll('.level-item[data-level-index]');

    levelItems.forEach((item, index) => {
        const level = drawdownLevels[index];
        if (!level || !level.historicalCases || level.historicalCases.length === 0) {
            return;
        }

        item.addEventListener('click', () => {
            showDetailView(level, ticker);
        });
    });
}

function showDetailView(level, ticker, pushState = true) {
    const results = document.getElementById('results');

    // Update URL if needed
    if (pushState) {
        const url = new URL(window.location);
        url.searchParams.set('ticker', ticker);
        url.searchParams.set('level', level.drawdownLevel);
        window.history.pushState({ ticker, level: level.drawdownLevel }, '', url);
    }

    // Hide main content
    const mainContent = results.querySelector('.stock-card');
    if (mainContent) {
        mainContent.style.display = 'none';
    }

    // Remove existing detail view if any
    const existingDetail = results.querySelector('.detail-view');
    if (existingDetail) {
        existingDetail.remove();
    }

    // Create detail view
    const detailView = document.createElement('div');
    detailView.className = 'detail-view show';

    const caseItems = level.historicalCases.map((histCase, index) => {
        const chartId = `detail-chart-${ticker}-${index}`;
        const duration = calculateDuration(histCase.startDate, histCase.bottomDate);
        return `
            <div class="detail-case-item">
                <div class="detail-case-header">
                    <div class="detail-case-date">
                        ${histCase.startDate} → ${histCase.bottomDate} (${duration})
                    </div>
                    <div class="detail-case-drawdown ${histCase.drawdownPercent >= 0 ? 'positive' : 'negative'}">
                        하락률: ${formatPercent(histCase.drawdownPercent)}
                    </div>
                </div>

                ${histCase.chartData ? `
                    <div class="detail-chart-container">
                        <canvas id="${chartId}" width="800" height="400"></canvas>
                    </div>
                ` : ''}

                <div class="detail-recovery-section">
                    <div class="detail-recovery-title">해당 저점에서 투자 시 수익률</div>
                    <div class="recovery-grid">
                        ${histCase.recoveryPeriods.map(period => `
                            <div class="recovery-item">
                                <div class="recovery-months">${period.months}개월 후</div>
                                <div class="recovery-return ${period.returnPercent >= 0 ? 'positive' : 'negative'}">
                                    ${formatPercent(period.returnPercent)}
                                </div>
                            </div>
                        `).join('')}
                    </div>
                </div>
            </div>
        `;
    }).join('');

    detailView.innerHTML = `
        <div class="detail-header">
            <h2 class="detail-title">${ticker} - ${level.drawdownLevel}% 하락 시 과거 사례 (총 ${level.totalCases}건)</h2>
            <button class="back-button" onclick="hideDetailView()">← 돌아가기</button>
        </div>
        <div class="detail-cases">
            ${caseItems}
        </div>
    `;

    results.appendChild(detailView);

    // Draw charts
    setTimeout(() => {
        level.historicalCases.forEach((histCase, index) => {
            if (histCase.chartData) {
                const chartId = `detail-chart-${ticker}-${index}`;
                createDetailChart(chartId, histCase);
            }
        });
    }, 100);

    // Scroll to top
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function hideDetailView(pushState = true) {
    const results = document.getElementById('results');
    const detailView = results.querySelector('.detail-view');
    const mainContent = results.querySelector('.stock-card');

    // Update URL if needed
    if (pushState) {
        const url = new URL(window.location);
        url.searchParams.delete('level');
        window.history.pushState({ ticker: currentTicker }, '', url);
    }

    if (detailView) {
        detailView.remove();
    }

    if (mainContent) {
        mainContent.style.display = 'block';
    }

    // Scroll to top
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function createDetailChart(canvasId, historicalDrawdown) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const chartData = historicalDrawdown.chartData;

    if (!chartData || !chartData.labels || !chartData.prices) {
        return;
    }

    const labels = chartData.labels;
    const prices = chartData.prices.map(p => parseFloat(p));
    const peakPrice = parseFloat(chartData.peakPrice);
    const peakDate = chartData.peakDate;
    const bottomDate = historicalDrawdown.bottomDate;

    // Find indices for peak and bottom
    const peakIndex = labels.indexOf(peakDate);
    const bottomIndex = labels.indexOf(bottomDate);

    // Create scatter data for peak and bottom points
    const peakPointData = labels.map((label, index) => {
        if (index === peakIndex) return prices[index];
        return null;
    });

    const bottomPointData = labels.map((label, index) => {
        if (index === bottomIndex) return prices[index];
        return null;
    });

    // Calculate recovery period points (1, 3, 6, 12, 24 months after bottom)
    const recoveryPoints = {};
    const recoveryMonths = [1, 3, 6, 12, 24];
    const bottomDateObj = new Date(bottomDate);

    recoveryMonths.forEach(months => {
        const targetDate = new Date(bottomDateObj);
        targetDate.setMonth(targetDate.getMonth() + months);

        // Find closest date in labels
        let closestIndex = -1;
        let minDiff = Infinity;
        labels.forEach((label, index) => {
            if (index >= bottomIndex) {
                const labelDate = new Date(label);
                const diff = Math.abs(labelDate - targetDate);
                if (diff < minDiff) {
                    minDiff = diff;
                    closestIndex = index;
                }
            }
        });

        if (closestIndex >= 0) {
            recoveryPoints[months] = labels.map((label, index) => {
                return index === closestIndex ? prices[index] : null;
            });
        }
    });

    // Prepare datasets
    const datasets = [
        {
            label: '가격',
            data: prices,
            borderColor: '#6c757d',
            backgroundColor: 'rgba(108, 117, 125, 0.05)',
            borderWidth: 2,
            fill: true,
            tension: 0.1,
            pointRadius: 0,
            pointHoverRadius: 4
        },
        {
            label: '고점',
            data: peakPointData,
            type: 'scatter',
            backgroundColor: '#198754',
            pointRadius: 6,
            pointHoverRadius: 8
        },
        {
            label: '저점',
            data: bottomPointData,
            type: 'scatter',
            backgroundColor: '#dc3545',
            pointRadius: 6,
            pointHoverRadius: 8
        }
    ];

    // Add recovery period markers
    const recoveryColors = {
        1: '#ffc107',   // amber
        3: '#ff9800',   // orange
        6: '#ff5722',   // deep orange
        12: '#9c27b0',  // purple
        24: '#3f51b5'   // indigo
    };

    recoveryMonths.forEach(months => {
        if (recoveryPoints[months]) {
            datasets.push({
                label: `+${months}개월`,
                data: recoveryPoints[months],
                type: 'scatter',
                backgroundColor: recoveryColors[months],
                borderColor: recoveryColors[months],
                borderWidth: 2,
                pointRadius: 6,
                pointHoverRadius: 8,
                showLine: false
            });
        }
    });

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: true,
                    position: 'top'
                },
                tooltip: {
                    mode: 'index',
                    intersect: false
                }
            },
            scales: {
                x: {
                    display: true,
                    ticks: {
                        maxRotation: 45,
                        minRotation: 45,
                        font: {
                            size: 10
                        }
                    }
                },
                y: {
                    display: true,
                    ticks: {
                        callback: function(value) {
                            return '$' + value.toFixed(2);
                        }
                    }
                }
            }
        }
    });
}

// Utility Functions
function formatNumber(num) {
    if (num === null || num === undefined) return 'N/A';
    return Number(num).toFixed(2);
}

function formatPercent(num) {
    if (num === null || num === undefined) return 'N/A';
    const value = Number(num);
    const sign = value >= 0 ? '+' : '';
    return `${sign}${value.toFixed(2)}%`;
}

function calculateDuration(startDateStr, endDateStr) {
    const startDate = new Date(startDateStr);
    const endDate = new Date(endDateStr);

    let years = endDate.getFullYear() - startDate.getFullYear();
    let months = endDate.getMonth() - startDate.getMonth();
    let days = endDate.getDate() - startDate.getDate();

    // Adjust for negative days
    if (days < 0) {
        months--;
        // Get days in previous month
        const prevMonth = new Date(endDate.getFullYear(), endDate.getMonth(), 0);
        days += prevMonth.getDate();
    }

    // Adjust for negative months
    if (months < 0) {
        years--;
        months += 12;
    }

    // Format the result
    const parts = [];
    if (years > 0) parts.push(`${years}년`);
    if (months > 0) parts.push(`${months}개월`);
    if (days > 0) parts.push(`${days}일`);

    return parts.length > 0 ? parts.join(' ') : '0일';
}

// Chart Functions
function createPriceChart(ticker, chartData, drawdown) {
    const canvas = document.getElementById(`chart-${ticker}`);
    if (!canvas) return;

    const ctx = canvas.getContext('2d');

    // Prepare data
    const labels = chartData.labels;
    const prices = chartData.prices.map(p => parseFloat(p));
    const peakPrice = parseFloat(chartData.peakPrice);
    const currentPrice = parseFloat(drawdown.currentPrice);

    // Create peak line data (horizontal line)
    const peakLineData = labels.map(() => peakPrice);

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'Price',
                    data: prices,
                    borderColor: '#2c5282',
                    backgroundColor: 'rgba(44, 82, 130, 0.08)',
                    borderWidth: 2,
                    fill: true,
                    tension: 0.1,
                    pointRadius: 0,
                    pointHoverRadius: 4
                },
                {
                    label: `Peak ($${peakPrice.toFixed(2)})`,
                    data: peakLineData,
                    borderColor: '#198754',
                    borderWidth: 1.5,
                    borderDash: [5, 5],
                    fill: false,
                    pointRadius: 0,
                    pointHoverRadius: 0
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            interaction: {
                intersect: false,
                mode: 'index'
            },
            plugins: {
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        color: '#2d3748',
                        font: {
                            size: 12
                        }
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    padding: 12,
                    titleColor: '#fff',
                    bodyColor: '#fff',
                    displayColors: true,
                    callbacks: {
                        label: function(context) {
                            let label = context.dataset.label || '';
                            if (label) {
                                label += ': ';
                            }
                            if (context.parsed.y !== null) {
                                label += '$' + context.parsed.y.toFixed(2);
                            }
                            return label;
                        }
                    }
                }
            },
            scales: {
                x: {
                    display: true,
                    grid: {
                        display: false
                    },
                    ticks: {
                        color: '#718096',
                        maxTicksLimit: 8,
                        font: {
                            size: 10
                        }
                    }
                },
                y: {
                    display: true,
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)'
                    },
                    ticks: {
                        color: '#718096',
                        callback: function(value) {
                            return '$' + value.toFixed(0);
                        },
                        font: {
                            size: 11
                        }
                    }
                }
            }
        }
    });
}

function createHistoricalDrawdownChart(chartId, historicalDrawdown) {
    const canvas = document.getElementById(chartId);
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const chartData = historicalDrawdown.chartData;

    // Prepare data
    const labels = chartData.labels;
    const prices = chartData.prices.map(p => parseFloat(p));
    const peakPrice = parseFloat(chartData.peakPrice);

    // Create peak line data (horizontal line)
    const peakLineData = labels.map(() => peakPrice);

    // Find peak and bottom dates for annotations
    const peakDate = chartData.peakDate;
    const bottomDate = historicalDrawdown.bottomDate;

    // Find indices for peak and bottom
    const peakIndex = labels.indexOf(peakDate);
    const bottomIndex = labels.indexOf(bottomDate);

    // Create scatter data for peak and bottom points
    const peakPointData = labels.map((label, index) => {
        if (index === peakIndex) {
            return prices[index];
        }
        return null;
    });

    const bottomPointData = labels.map((label, index) => {
        if (index === bottomIndex) {
            return prices[index];
        }
        return null;
    });

    // Calculate recovery period points (1, 3, 6, 12, 24 months after bottom)
    const recoveryPoints = {};
    const recoveryMonths = [1, 3, 6, 12, 24];
    const bottomDateObj = new Date(bottomDate);

    recoveryMonths.forEach(months => {
        const targetDate = new Date(bottomDateObj);
        targetDate.setMonth(targetDate.getMonth() + months);
        const targetDateStr = targetDate.toISOString().split('T')[0];

        // Find closest date in labels
        let closestIndex = -1;
        let minDiff = Infinity;
        labels.forEach((label, index) => {
            if (index >= bottomIndex) {
                const labelDate = new Date(label);
                const diff = Math.abs(labelDate - targetDate);
                if (diff < minDiff) {
                    minDiff = diff;
                    closestIndex = index;
                }
            }
        });

        if (closestIndex >= 0) {
            recoveryPoints[months] = labels.map((label, index) => {
                return index === closestIndex ? prices[index] : null;
            });
        }
    });

    // Prepare datasets
    const datasets = [
        {
            label: '가격',
            data: prices,
            borderColor: '#6c757d',
            backgroundColor: 'rgba(108, 117, 125, 0.08)',
            borderWidth: 2,
            fill: true,
            tension: 0.1,
            pointRadius: 0,
            pointHoverRadius: 4
        },
        {
            label: `고점 ($${peakPrice.toFixed(2)})`,
            data: peakLineData,
            borderColor: '#198754',
            borderWidth: 1,
            borderDash: [3, 3],
            fill: false,
            pointRadius: 0,
            pointHoverRadius: 0
        },
        {
            label: '고점 시점',
            data: peakPointData,
            type: 'scatter',
            backgroundColor: '#198754',
            borderColor: '#198754',
            borderWidth: 2,
            pointRadius: 7,
            pointHoverRadius: 9,
            showLine: false
        },
        {
            label: '저점 시점',
            data: bottomPointData,
            type: 'scatter',
            backgroundColor: '#dc3545',
            borderColor: '#dc3545',
            borderWidth: 2,
            pointRadius: 7,
            pointHoverRadius: 9,
            showLine: false
        }
    ];

    // Add recovery period markers
    const recoveryColors = {
        1: '#ffc107',   // amber
        3: '#ff9800',   // orange
        6: '#ff5722',   // deep orange
        12: '#9c27b0',  // purple
        24: '#3f51b5'   // indigo
    };

    recoveryMonths.forEach(months => {
        if (recoveryPoints[months]) {
            datasets.push({
                label: `+${months}개월`,
                data: recoveryPoints[months],
                type: 'scatter',
                backgroundColor: recoveryColors[months],
                borderColor: recoveryColors[months],
                borderWidth: 2,
                pointRadius: 6,
                pointHoverRadius: 8,
                showLine: false
            });
        }
    });

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            interaction: {
                intersect: false,
                mode: 'index'
            },
            plugins: {
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        color: '#2d3748',
                        font: {
                            size: 10
                        },
                        usePointStyle: true,
                        padding: 8
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    padding: 8,
                    titleColor: '#fff',
                    bodyColor: '#fff',
                    displayColors: true,
                    callbacks: {
                        label: function(context) {
                            let label = context.dataset.label || '';
                            if (label) {
                                label += ': ';
                            }
                            if (context.parsed.y !== null) {
                                label += '$' + context.parsed.y.toFixed(2);
                            }
                            return label;
                        }
                    }
                }
            },
            scales: {
                x: {
                    display: true,
                    grid: {
                        display: false
                    },
                    ticks: {
                        color: '#718096',
                        maxTicksLimit: 6,
                        font: {
                            size: 9
                        }
                    }
                },
                y: {
                    display: true,
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)'
                    },
                    ticks: {
                        color: '#718096',
                        callback: function(value) {
                            return '$' + value.toFixed(0);
                        },
                        font: {
                            size: 10
                        }
                    }
                }
            }
        }
    });
}

// Load default stock on page load
window.addEventListener('load', () => {
    analyzeStock(tickerSelect.value);
});
