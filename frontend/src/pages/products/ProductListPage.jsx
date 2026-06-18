import { useEffect, useState } from "react";
// 검색 조건을 URL에 남기고 상품 상세 링크를 연결하기 위해 라우터 기능을 사용함
import { Link, useSearchParams } from "react-router";
// 목록 화면에서 카드별 장바구니 담기를 바로 처리하기 위해 공통 API를 사용함
import { getCurrentUser } from "../auth/authApi";
import { addCartItem } from "../cart/cartApi";
import { getProducts, getProductsByCategory, searchProducts } from "./productApi";
// 상품 이미지와 가격 표시 규칙을 상세 화면과 동일하게 맞춤
import { formatPrice, isImageUrl } from "./productDisplay";
import "./ProductListPage.css";

// 목록 검색 카테고리는 Thymeleaf shop.html의 선택지와 맞춤
const CATEGORY_OPTIONS = ["비누", "목공", "패브릭", "문구"];

function ProductListPage() {
    // 검색 조건을 주소창 query string과 동기화해 새로고침해도 같은 결과가 보이게 함
    const [searchParams, setSearchParams] = useSearchParams();
    const keywordParam = searchParams.get("keyword") ?? "";
    const categoryParam = searchParams.get("category") ?? "";

    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState("");
    // 목록 카드마다 다른 수량을 선택할 수 있게 상품 ID 기준으로 수량을 저장함
    const [itemQuantities, setItemQuantities] = useState({});
    // 카드 장바구니 요청 결과를 목록 상단에서 안내함
    const [cartMessage, setCartMessage] = useState({ type: "", text: "" });
    // 동시에 여러 상품을 담지 않도록 현재 요청 중인 상품 ID를 저장함
    const [cartLoadingProductId, setCartLoadingProductId] = useState(null);

    useEffect(() => {
        async function fetchProducts() {
            try {
                setLoading(true);
                setErrorMessage("");

                // URL 조건에 따라 전체/검색/카테고리 API 중 하나를 호출함
                const keyword = keywordParam.trim();
                const data = keyword
                    ? await searchProducts(keyword)
                    : categoryParam
                        ? await getProductsByCategory(categoryParam)
                        : await getProducts();
                setProducts(data);
                // 새 목록을 불러올 때 기존 수량을 유지하되 새 상품은 1개로 초기화함
                setItemQuantities((currentQuantities) => {
                    const nextQuantities = {};
                    data.forEach((product) => {
                        nextQuantities[product.productId] = currentQuantities[product.productId] ?? 1;
                    });
                    return nextQuantities;
                });
            } catch (error) {
                console.error(error);
                setErrorMessage("상품 목록을 불러오지 못했습니다.");
            } finally {
                setLoading(false);
            }
        }

        fetchProducts();
    }, [keywordParam, categoryParam]);

    // 검색 폼 제출 시 FormData로 입력값을 읽어 URL query로 옮김
    function handleSearchSubmit(event) {
        event.preventDefault();
        const nextParams = new URLSearchParams();
        const formData = new FormData(event.currentTarget);
        const selectedCategory = formData.get("category")?.toString() ?? "";
        const keyword = formData.get("keyword")?.toString().trim() ?? "";

        if (selectedCategory) {
            nextParams.set("category", selectedCategory);
        }
        if (keyword) {
            nextParams.set("keyword", keyword);
        }

        setCartMessage({ type: "", text: "" });
        setSearchParams(nextParams);
    }

    // 전체보기 버튼은 URL 조건을 비워 폼과 목록을 전체 상품 기준으로 되돌림
    function handleResetSearch() {
        setCartMessage({ type: "", text: "" });
        setSearchParams(new URLSearchParams());
    }

    // 카드 수량 input 변경값을 1 이상, 재고 이하로 보정해 잘못된 장바구니 요청을 막음
    function handleQuantityChange(product, value) {
        const maxQuantity = Math.max(1, product.quantity ?? 1);
        const nextQuantity = Math.min(maxQuantity, Math.max(1, Number(value) || 1));

        setItemQuantities((currentQuantities) => ({
            ...currentQuantities,
            [product.productId]: nextQuantity,
        }));
    }

    // 목록 카드에서 바로 장바구니에 담기 위해 로그인 사용자 확인 후 장바구니 API를 호출함
    async function handleAddToCart(product) {
        if (isProductSoldOut(product)) {
            setCartMessage({ type: "error", text: "품절된 상품은 장바구니에 담을 수 없습니다." });
            return;
        }

        try {
            setCartLoadingProductId(product.productId);
            setCartMessage({ type: "", text: "" });

            const currentUser = await getCurrentUser();
            if (!currentUser?.id) {
                throw new Error("LOGIN_REQUIRED");
            }

            const quantity = itemQuantities[product.productId] ?? 1;
            await addCartItem(currentUser.id, product.productId, quantity);

            setCartMessage({
                type: "success",
                text: `${product.productName} ${quantity}개를 장바구니에 담았습니다.`,
            });
        } catch (error) {
            console.error(error);
            setCartMessage({
                type: "error",
                text: "로그인 후 장바구니를 이용할 수 있습니다.",
            });
        } finally {
            setCartLoadingProductId(null);
        }
    }

    // 품절 판정을 한 함수로 모아 목록 카드와 버튼 비활성 조건을 같게 유지함
    function isProductSoldOut(product) {
        return product.isSoldOut || product.quantity === 0;
    }

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

            {/* Thymeleaf shop.html의 검색 폼을 React 라우터 query 기반으로 옮김 */}
            <form
                className="product-search-form"
                key={`${categoryParam}:${keywordParam}`}
                onSubmit={handleSearchSubmit}
            >
                <label className="product-search-label" htmlFor="product-category">
                    카테고리
                </label>
                <select
                    id="product-category"
                    name="category"
                    defaultValue={categoryParam}
                >
                    <option value="">전체</option>
                    {CATEGORY_OPTIONS.map((category) => (
                        <option key={category} value={category}>
                            {category}
                        </option>
                    ))}
                </select>

                <label className="product-search-label" htmlFor="product-keyword">
                    검색어
                </label>
                <input
                    id="product-keyword"
                    name="keyword"
                    type="search"
                    defaultValue={keywordParam}
                    placeholder="찾고 싶은 핸드메이드 제품을 검색해보세요"
                />

                <button className="product-search-button" type="submit">
                    검색
                </button>
                <button className="product-reset-button" type="button" onClick={handleResetSearch}>
                    전체 보기
                </button>
            </form>

            {/* 현재 적용된 검색 조건과 장바구니 처리 결과를 목록 상단에 보여줌 */}
            <div className="product-list-status">
                <span>
                    {keywordParam || categoryParam
                        ? `검색 조건: ${keywordParam || "전체"} / ${categoryParam || "전체 카테고리"}`
                        : "전체 상품을 표시합니다."}
                </span>
                {cartMessage.text && (
                    <strong className={`product-cart-message ${cartMessage.type}`}>
                        {cartMessage.text}
                    </strong>
                )}
            </div>

            {products.length === 0 ? (
                <p className="empty-message">등록된 상품이 없습니다.</p>
            ) : (
                <div className="product-grid">
                    {products.map((product) => (
                        // 카드 내부에 장바구니 버튼을 두기 위해 상세 링크와 액션 영역을 분리함
                        <article className="product-card" key={product.productId}>
                            <Link
                                className="product-card-link"
                                to={`/products/${product.productId}`}
                                aria-label={`${product.productName} 상세 보기`}
                            >
                                <div className="product-image-box">
                                    {isImageUrl(product.mainImage) ? (
                                        <img
                                            src={product.mainImage}
                                            alt={product.productName}
                                            className="product-image"
                                        />
                                    ) : (
                                        <div className="product-image-placeholder">
                                            {product.category || "No Image"}
                                        </div>
                                    )}
                                </div>
                            </Link>

                            <div className="product-info">
                                <div className="product-category">{product.category}</div>

                                <Link className="product-name-link" to={`/products/${product.productId}`}>
                                    <h3 className="product-name">{product.productName}</h3>
                                </Link>

                                <p className="product-seller">
                                    판매자: {product.sellerName || "알 수 없음"}
                                </p>

                                <div className="product-bottom">
                                    <strong className="product-price">
                                        {formatPrice(product.price)}
                                    </strong>

                                    {isProductSoldOut(product) ? (
                                        <span className="sold-out-badge">품절</span>
                                    ) : (
                                        <span className="stock-badge">재고 {product.quantity}개</span>
                                    )}
                                </div>

                                {/* 목록 카드에서 수량 선택 후 바로 장바구니에 담을 수 있게 함 */}
                                <div className="product-card-actions">
                                    <input
                                        className="product-card-quantity"
                                        type="number"
                                        min="1"
                                        max={product.quantity ?? 1}
                                        value={itemQuantities[product.productId] ?? 1}
                                        onChange={(event) => handleQuantityChange(product, event.target.value)}
                                        aria-label={`${product.productName} 수량`}
                                        disabled={isProductSoldOut(product)}
                                    />
                                    <button
                                        className="product-card-cart-button"
                                        type="button"
                                        onClick={() => handleAddToCart(product)}
                                        disabled={isProductSoldOut(product) || cartLoadingProductId === product.productId}
                                    >
                                        {cartLoadingProductId === product.productId ? "담는 중" : "담기"}
                                    </button>
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
