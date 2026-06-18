import api from "../../shared/api/axios";

export async function getProducts() {
    const response = await api.get("/api/products");
    return response.data;
}

// 카테고리 선택 시 백엔드 카테고리 조회 API를 호출하도록 분리함
export async function getProductsByCategory(category) {
    const response = await api.get(`/api/products/category/${encodeURIComponent(category)}`);
    return response.data;
}

// 검색어 입력 시 상품명 검색 API를 호출하도록 분리함
export async function searchProducts(keyword) {
    const response = await api.get("/api/products/search", {
        params: { keyword },
    });
    return response.data;
}

export async function getProduct(productId) {
    const response = await api.get(`/api/products/${productId}`);
    return response.data;
}
