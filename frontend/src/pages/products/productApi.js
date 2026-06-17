import api from "../../shared/api/axios";

export async function getProducts() {
    const response = await api.get("/api/products");
    return response.data;
}