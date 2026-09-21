import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const BASE_URL = requiredEnvironmentValue('BASE_URL').replace(/\/+$/, '');
const TEST_STAGE = requiredEnvironmentValue('TEST_STAGE').toLowerCase();
const PETITION_IDS = requiredEnvironmentValue('PETITION_IDS')
    .split(',')
    .map((value) => value.trim())
    .filter((value) => /^\d+$/.test(value));

if (PETITION_IDS.length === 0) {
    throw new Error('PETITION_IDS must contain at least one existing numeric petition ID.');
}

const LOAD_PROFILES = {
    smoke: { vus: 1, duration: '1m' },
    5: { vus: 5, duration: '3m' },
    10: { vus: 10, duration: '3m' },
    30: { vus: 30, duration: '5m' },
    50: { vus: 50, duration: '3m' },
    80: { vus: 80, duration: '3m' },
};
const SELECTED_PROFILE = LOAD_PROFILES[TEST_STAGE];

if (!SELECTED_PROFILE) {
    throw new Error('TEST_STAGE must be one of: smoke, 5, 10, 30, 50, 80.');
}

const status400 = new Counter('http_status_400');
const status401 = new Counter('http_status_401');
const status403 = new Counter('http_status_403');
const status404 = new Counter('http_status_404');
const status429 = new Counter('http_status_429');
const status500 = new Counter('http_status_500');
const status502 = new Counter('http_status_502');
const status503 = new Counter('http_status_503');
const status504 = new Counter('http_status_504');
const otherFailureStatus = new Counter('http_status_other_failure');
const serverErrorCount = new Counter('http_5xx_count');
const serverErrorRate = new Rate('http_5xx_rate');

export const options = {
    scenarios: {
        selected_stage: {
            executor: 'constant-vus',
            exec: 'readOnlyUserFlow',
            vus: SELECTED_PROFILE.vus,
            duration: SELECTED_PROFILE.duration,
            gracefulStop: '0s',
            tags: {
                test_stage: TEST_STAGE,
            },
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<2000', 'p(99)<4000'],
        http_5xx_count: ['count==0'],
        http_5xx_rate: ['rate==0'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export function readOnlyUserFlow() {
    const selection = Math.random();
    const petitionId = randomPetitionId();

    let response;
    let endpoint;

    if (selection < 0.40) {
        endpoint = 'petition_list';
        response = http.get(
            `${BASE_URL}/connect/petitions?page=0&size=20&sort=createdAt%2Cdesc`,
            requestParameters(endpoint)
        );
    } else if (selection < 0.70) {
        endpoint = 'petition_detail';
        response = http.get(
            `${BASE_URL}/connect/petitions/${petitionId}`,
            requestParameters(endpoint)
        );
    } else if (selection < 0.95) {
        endpoint = 'comment_list';
        response = http.get(
            `${BASE_URL}/connect/petitions/${petitionId}/comments?page=0&size=20`,
            requestParameters(endpoint)
        );
    } else {
        endpoint = 'notice_list';
        response = http.get(
            `${BASE_URL}/connect/notices?page=0&size=20`,
            requestParameters(endpoint)
        );
    }

    check(response, {
        [`${endpoint} returns 200`]: (result) => result.status === 200,
    });
    recordFailureStatus(response.status);

    sleep(1 + Math.random() * 2);
}

function requestParameters(endpoint) {
    return {
        headers: {
            Accept: 'application/json',
        },
        tags: {
            endpoint,
        },
        timeout: '10s',
    };
}

function randomPetitionId() {
    return PETITION_IDS[Math.floor(Math.random() * PETITION_IDS.length)];
}

function recordFailureStatus(status) {
    const serverError = status >= 500 && status <= 599;
    serverErrorRate.add(serverError);

    if (status >= 200 && status <= 399) {
        return;
    }

    switch (status) {
        case 400:
            status400.add(1);
            break;
        case 401:
            status401.add(1);
            break;
        case 403:
            status403.add(1);
            break;
        case 404:
            status404.add(1);
            break;
        case 429:
            status429.add(1);
            break;
        case 500:
            status500.add(1);
            serverErrorCount.add(1);
            break;
        case 502:
            status502.add(1);
            serverErrorCount.add(1);
            break;
        case 503:
            status503.add(1);
            serverErrorCount.add(1);
            break;
        case 504:
            status504.add(1);
            serverErrorCount.add(1);
            break;
        default:
            otherFailureStatus.add(1);
            if (serverError) {
                serverErrorCount.add(1);
            }
    }
}

function requiredEnvironmentValue(name) {
    const value = __ENV[name];
    if (!value || value.trim() === '') {
        throw new Error(`${name} environment variable is required.`);
    }
    return value.trim();
}
