import api from "../../shared/api/axios";

// 현재 로그인 사용자의 주문 목록만 조회함
export async function getMyOrders() {
    const response = await api.get("/api/orders/me");
    return response.data;
}

// 주문 생성은 서버가 장바구니를 기준으로 처리하므로 수령 정보만 전달함
export async function createOrder(orderForm) {
    const response = await api.post("/api/orders", orderForm);
    return response.data;
}

// 주문 수정은 사용자가 바꿀 수 있는 수령 정보만 전달함
export async function updateOrder(orderId, orderForm) {
    const response = await api.put(`/api/orders/${orderId}`, orderForm);
    return response.data;
}

// 주문 취소는 삭제가 아니라 서버의 취소 상태 변경 API를 호출함
export async function cancelOrder(orderId) {
    const response = await api.patch(`/api/orders/${orderId}/cancel`);
    return response.data;
}
