import { useEffect, useState } from "react";
import { getProducts } from "./productApi";
import "./ProductListPage.css";

function ProductListPage() {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState("");

    useEffect(() => {
        async function fetchProducts() {
            try {
                setLoading(true);
                setErrorMessage("");

                const data = await getProducts();
                setProducts(data);
            } catch (error) {
                console.error(error);
                setErrorMessage("상품 목록을 불러오지 못했습니다.");
            } finally {
                setLoading(false);
            }
        }

        fetchProducts();
    }, []);

    if (loading) {
        return (
            <section className="product-list-page">
                <h2>상품 목록</h2>
                <p>상품을 불러오는 중입니다.</p>
            </section>
        );
    }

    if (errorMessage) {
        return (
            <section className="product-list-page">
                <h2>상품 목록</h2>
                <p className="error-message">{errorMessage}</p>
            </section>
        );
    }

    return (
        <section className="product-list-page">
            <div className="product-list-header">
                <div>
                    <h2>상품 목록</h2>
                    <p>핸드메이드 상품을 둘러보세요.</p>
                </div>
                <span>총 {products.length}개</span>
            </div>

            {products.length === 0 ? (
                <p className="empty-message">등록된 상품이 없습니다.</p>
            ) : (
                <div className="product-grid">
                    {products.map((product) => (
                        <article className="product-card" key={product.productId}>
                            <div className="product-image-box">
                                {product.mainImage ? (
                                    <img
                                        src={product.mainImage}
                                        alt={product.productName}
                                        className="product-image"
                                    />
                                ) : (
                                    <div className="product-image-placeholder">No Image</div>
                                )}
                            </div>

                            <div className="product-info">
                                <div className="product-category">{product.category}</div>

                                <h3 className="product-name">{product.productName}</h3>

                                <p className="product-seller">
                                    판매자: {product.sellerName || "알 수 없음"}
                                </p>

                                <div className="product-bottom">
                                    <strong className="product-price">
                                        {product.price?.toLocaleString()}원
                                    </strong>

                                    {product.isSoldOut || product.quantity === 0 ? (
                                        <span className="sold-out-badge">품절</span>
                                    ) : (
                                        <span className="stock-badge">재고 {product.quantity}개</span>
                                    )}
                                </div>
                            </div>
                        </article>
                    ))}
                </div>
            )}
        </section>
    );
}

export default ProductListPage;