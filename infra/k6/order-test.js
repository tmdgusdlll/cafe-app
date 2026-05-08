import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    // stages: [
    //     { duration: '30s', target: 50 },  // 워밍업 (적은 사용자로 시작)
    //     { duration: '1m', target: 50 },  // 본 테스트
    //     { duration: '30s', target: 0 },   // 종료
    // ],
    vus: 100,
    duration: '10s',
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<500'],
    },
};

export default function () {
    // 비관적 락
    // const url = 'http://nginx/users/1/order';
    // 분산 락
    const url = 'http://nginx/users/1/order/redisson';
    // 단일 인스턴스
    // const url = 'http://host.docker.internal:8080/users/1/order/redisson';

    const payload = JSON.stringify({
        menuId: 1,
        quantity: 1,
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const res = http.post(url, payload, params);

    if (res.status !== 201) {
        console.warn(`[Fail] VU: ${__VU}, Status: ${res.status}, Body: ${res.body}`);
    }

    check(res, {
        '201 성공': (r) => r.status === 201,
        // '400 포인트부족': (r) => r.status === 400,
    });

    sleep(1);
}