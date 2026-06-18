import api from "../../shared/api/axios";

// 상품 상세에서 장바구니 추가 요청을 재사용할 수 있게 API 호출을 분리함
export async function addCartItem(userId, productId, quantity) {
    const response = await api.post(`/api/carts/${userId}/items`, {
        productId,
        quantity,
    });
    return response.data;
}
