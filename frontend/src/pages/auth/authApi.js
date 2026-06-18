import api from "../../shared/api/axios";

// React dev 서버와 백엔드 서버가 다르므로 OAuth/Thymeleaf 이동용 백엔드 주소를 한 곳에서 계산함
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";

// 현재 로그인 사용자 정보를 장바구니와 주문 화면에서 재사용하려고 API 함수로 분리함
export async function getCurrentUser() {
    const response = await api.get("/api/user/me");
    return response.data;
}

// 일반 로그인은 세션 생성을 위해 백엔드 signin API로 아이디와 비밀번호를 전달함
export async function signIn(loginForm) {
    const response = await api.post("/api/user/signin", loginForm);
    return response.data;
}

// 로그아웃은 React에서도 세션을 끊을 수 있게 JSON API를 사용함
export async function logoutUser() {
    const response = await api.post("/api/user/logout");
    return response.data;
}

// OAuth 로그인은 백엔드 Spring Security 엔드포인트로 브라우저를 직접 이동시킴
export function getOAuthLoginUrl(provider) {
    return getBackendUrl(`/oauth2/authorization/${provider}`);
}

// 아직 React로 옮기지 않은 회원가입 같은 화면은 백엔드 페이지로 이동할 수 있게 함
export function getServerPageUrl(path) {
    return getBackendUrl(path);
}

// 백엔드 기본 주소가 있으면 절대 URL로, 없으면 같은 origin 상대 경로로 처리함
function getBackendUrl(path) {
    if (!API_BASE_URL) {
        return path;
    }

    return new URL(path, API_BASE_URL).toString();
}
