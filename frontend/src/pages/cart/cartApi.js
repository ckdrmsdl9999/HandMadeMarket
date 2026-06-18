import api from "../../shared/api/axios";

// 로그인 사용자의 장바구니를 서버에서 조회함
export async function getCart(userId) {
    const response = await api.get(`/api/carts/${userId}`);
    return response.data;
}

// 상품 상세에서 장바구니 추가 요청을 재사용할 수 있게 API 호출을 분리함
export async function addCartItem(userId, productId, quantity) {
    const response = await api.post(`/api/carts/${userId}/items`, {
        productId,
        quantity,
    });
    return response.data;
}

// 장바구니 항목 수량을 서버 기준으로 갱신함
export async function updateCartItemQuantity(userId, cartItemId, quantity) {
    const response = await api.patch(`/api/carts/${userId}/items/${cartItemId}`, {
        quantity,
    });
    return response.data;
}

// 장바구니에서 단일 항목을 제거함
export async function removeCartItem(userId, cartItemId) {
    const response = await api.delete(`/api/carts/${userId}/items/${cartItemId}`);
    return response.data;
}

// 장바구니 항목 전체를 비워 초기 상태로 돌림
export async function clearCartItems(userId) {
    const response = await api.delete(`/api/carts/${userId}/items`);
    return response.data;
}
