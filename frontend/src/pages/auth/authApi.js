import api from "../../shared/api/axios";

// 현재 로그인 사용자 정보를 장바구니와 주문 화면에서 재사용하려고 API 함수로 분리함
export async function getCurrentUser() {
    const response = await api.get("/api/user/me");
    return response.data;
}
