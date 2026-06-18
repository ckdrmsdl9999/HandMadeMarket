import { Route, Routes } from "react-router";
import Layout from "../shared/components/Layout";
import HomePage from "../pages/home/HomePage";
import ProductListPage from "../pages/products/ProductListPage";
import ProductDetailPage from "../pages/products/ProductDetailPage";
import CartPage from "../pages/cart/CartPage";
import OrderListPage from "../pages/orders/OrderListPage";
import LoginPage from "../pages/auth/LoginPage";

function AppRouter() {
    return (
        <Routes>
            <Route element={<Layout />}>
                <Route path="/" element={<HomePage />} />
                <Route path="/products" element={<ProductListPage />} />
                <Route path="/products/:productId" element={<ProductDetailPage />} />
                <Route path="/cart" element={<CartPage />} />
                <Route path="/orders" element={<OrderListPage />} />
                <Route path="/login" element={<LoginPage />} />
            </Route>
        </Routes>
    );
}

export default AppRouter;