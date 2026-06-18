import { useEffect, useState } from "react";
// URL 상품 ID, 목록 이동, 바로구매 이동을 처리하기 위해 라우터 API를 사용함
import { Link, useNavigate, useParams } from "react-router";
// 장바구니는 로그인 사용자 ID가 필요하므로 현재 사용자 조회 API를 사용함
import { getCurrentUser } from "../auth/authApi";
// 상세 화면의 장바구니 담기 요청을 API 함수로 분리해 호출함
import { addCartItem } from "../cart/cartApi";
import { getProduct } from "./productApi";
// 상세 화면에서 상품 표시 규칙을 목록과 공유함
import { formatPrice, getProductDescription, isImageUrl } from "./productDisplay";
import "./ProductDetailPage.css";

// 상세 탭은 화면 상태 값과 라벨을 한 곳에서 관리해 버튼/내용 조건을 맞춤
const DETAIL_TABS = [
    { id: "description", label: "상품 상세설명" },
    { id: "shipping", label: "배송/교환/반품 안내" },
    { id: "reviews", label: "구매후기" },
    { id: "qna", label: "Q&A" },
];

function ProductDetailPage() {
    // 라우트의 productId로 어떤 상품을 조회할지 결정함
    const { productId } = useParams();
    // 바로구매 성공 후 장바구니 화면으로 이동하기 위해 navigate를 사용함
    const navigate = useNavigate();
    // 상세 조회 결과와 화면 상태를 분리해 로딩/오류 처리를 안정화함
    const [product, setProduct] = useState(null);
    const [loading, setLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState("");
    // 수량 입력은 React state로 관리해 화면 값과 요청 값을 항상 같게 유지함
    const [quantity, setQuantity] = useState(1);
    // 장바구니/바로구매 중 어떤 액션이 처리 중인지 구분해 버튼 문구와 중복 클릭을 제어함
    const [cartAction, setCartAction] = useState("");
    // 장바구니 처리 결과를 화면에 바로 보여주려고 메시지 state를 둠
    const [cartMessage, setCartMessage] = useState({ type: "", text: "" });
    // 담기 성공 후 장바구니 이동 여부를 묻는 모달 표시 상태를 관리함
    const [cartModalOpen, setCartModalOpen] = useState(false);
    // 상세/배송/리뷰/Q&A 탭 중 현재 열려 있는 탭을 관리함
    const [activeTab, setActiveTab] = useState("description");

    useEffect(() => {
        async function fetchProduct() {
            if (!Number.isInteger(Number(productId))) {
                setErrorMessage("잘못된 상품 경로입니다.");
                setLoading(false);
                return;
            }

            try {
                setLoading(true);
                setErrorMessage("");

                const data = await getProduct(productId);
                setProduct(data);
                // 다른 상품 상세로 이동했을 때 이전 수량과 메시지가 남지 않도록 초기화함
                setQuantity(1);
                setCartMessage({ type: "", text: "" });
                setCartModalOpen(false);
                setActiveTab("description");
            } catch (error) {
                console.error(error);
                setErrorMessage("상품 정보를 불러오지 못했습니다.");
            } finally {
                setLoading(false);
            }
        }

        // productId가 바뀔 때마다 상세 API를 다시 호출해 URL과 화면을 맞춤
        fetchProduct();
    }, [productId]);

    // 수량 감소 버튼은 1개 아래로 내려가지 않도록 제한함
    function decreaseQuantity() {
        setQuantity((currentQuantity) => Math.max(1, currentQuantity - 1));
    }

    // 수량 증가 버튼은 현재 재고보다 많이 선택하지 못하도록 제한함
    function increaseQuantity() {
        setQuantity((currentQuantity) => Math.min(product.quantity ?? 1, currentQuantity + 1));
    }

    // 장바구니 담기와 바로구매가 같은 검증/요청 흐름을 쓰도록 공통 함수로 묶음
    async function addProductToCart({ actionType, successMessage, openModal, redirectToCart }) {
        if (product.isSoldOut || product.quantity === 0) {
            setCartMessage({ type: "error", text: "품절된 상품은 장바구니에 담을 수 없습니다." });
            return false;
        }

        try {
            setCartAction(actionType);
            setCartMessage({ type: "", text: "" });

            const currentUser = await getCurrentUser();
            // 로그인 페이지 HTML 응답처럼 사용자 ID가 없는 경우를 장바구니 요청 전에 차단함
            if (!currentUser?.id) {
                throw new Error("LOGIN_REQUIRED");
            }
            await addCartItem(currentUser.id, product.productId, quantity);

            setCartMessage({ type: "success", text: successMessage });
            if (openModal) {
                setCartModalOpen(true);
            }
            if (redirectToCart) {
                navigate("/cart");
            }
            return true;
        } catch (error) {
            console.error(error);
            setCartMessage({
                type: "error",
                text: "로그인 후 장바구니를 이용할 수 있습니다.",
            });
            return false;
        } finally {
            setCartAction("");
        }
    }

    // 일반 장바구니 담기는 성공 모달을 열어 계속 쇼핑할지 선택하게 함
    function handleAddToCart() {
        addProductToCart({
            actionType: "cart",
            successMessage: "장바구니에 상품을 담았습니다.",
            openModal: true,
            redirectToCart: false,
        });
    }

    // 바로구매는 기존 Thymeleaf처럼 장바구니에 담은 뒤 장바구니 화면으로 이동함
    function handleBuyNow() {
        addProductToCart({
            actionType: "buy",
            successMessage: "장바구니에 담고 이동합니다.",
            openModal: false,
            redirectToCart: true,
        });
    }

    if (loading) {
        return (
            <section className="product-detail-page">
                <h2>상품 상세</h2>
                <p>상품 정보를 불러오는 중입니다.</p>
            </section>
        );
    }

    if (errorMessage) {
        return (
            <section className="product-detail-page">
                <Link className="back-button" to="/products">목록으로</Link>
                <h2>상품 상세</h2>
                <p className="error-message">{errorMessage}</p>
            </section>
        );
    }

    // 상품 재고 상태를 한 번만 계산해 상세 배지와 안내 문구에 같이 사용함
    const isSoldOut = product.isSoldOut || product.quantity === 0;
    const description = getProductDescription(product);
    // 장바구니 버튼은 품절이거나 요청 중일 때 비활성화함
    const cartButtonDisabled = isSoldOut || cartAction !== "";

    return (
        <section className="product-detail-page">
            <Link className="back-button" to="/products">목록으로</Link>

            <article className="product-detail-card">
                <div className="product-detail-image-box">
                    {isImageUrl(product.mainImage) ? (
                        <img
                            src={product.mainImage}
                            alt={product.productName}
                            className="product-detail-image"
                        />
                    ) : (
                        <div className="product-detail-image-placeholder">
                            {product.category || "HandMade"}
                        </div>
                    )}
                </div>

                <div className="product-detail-info">
                    <p className="product-detail-category">{product.category || "미분류"}</p>
                    <h2>{product.productName}</h2>
                    <p className="product-detail-seller">
                        판매자: {product.sellerName || "알 수 없음"}
                    </p>
                    <strong className="product-detail-price">{formatPrice(product.price)}</strong>

                    <div className="product-detail-status">
                        {isSoldOut ? (
                            <span className="sold-out-badge">품절</span>
                        ) : (
                            <span className="stock-badge">재고 {product.quantity}개</span>
                        )}
                    </div>

                    <p className="product-detail-description">{description}</p>

                    {/* 선택 수량을 state와 연결해 버튼 클릭마다 화면이 다시 그려지게 함 */}
                    <div className="quantity-section">
                        <span className="quantity-label">수량</span>
                        <div className="quantity-control">
                            <button
                                className="quantity-button"
                                type="button"
                                onClick={decreaseQuantity}
                                disabled={quantity <= 1 || isSoldOut}
                            >
                                -
                            </button>
                            <span className="quantity-value">{quantity}</span>
                            <button
                                className="quantity-button"
                                type="button"
                                onClick={increaseQuantity}
                                disabled={quantity >= (product.quantity ?? 1) || isSoldOut}
                            >
                                +
                            </button>
                        </div>
                    </div>

                    {/* 장바구니 요청 결과를 같은 위치에서 안내해 사용자가 다음 행동을 알 수 있게 함 */}
                    <div className="product-detail-actions">
                        <button
                            className="cart-button"
                            type="button"
                            onClick={handleAddToCart}
                            disabled={cartButtonDisabled}
                        >
                            {cartAction === "cart" ? "담는 중..." : "장바구니 담기"}
                        </button>
                        <button
                            className="buy-now-button"
                            type="button"
                            onClick={handleBuyNow}
                            disabled={cartButtonDisabled}
                        >
                            {cartAction === "buy" ? "이동 중..." : "바로 구매하기"}
                        </button>
                    </div>

                    {/* 에러와 성공 메시지를 같은 구조로 보여주되 색상은 타입으로 구분함 */}
                    {cartMessage.text && (
                        <p className={`cart-message ${cartMessage.type}`}>
                            {cartMessage.text}
                        </p>
                    )}

                    <div className="product-detail-meta">
                        <p>상품번호: {product.productId}</p>
                        <p>판매 수량: {product.salesCount ?? 0}개</p>
                    </div>
                </div>
            </article>

            {/* Thymeleaf 상세의 탭 영역을 React state 기반으로 옮김 */}
            <section className="product-detail-tabs">
                <div className="detail-tab-list" role="tablist" aria-label="상품 상세 정보">
                    {DETAIL_TABS.map((tab) => (
                        <button
                            className={`detail-tab-button ${activeTab === tab.id ? "active" : ""}`}
                            key={tab.id}
                            type="button"
                            role="tab"
                            aria-selected={activeTab === tab.id}
                            onClick={() => setActiveTab(tab.id)}
                        >
                            {tab.label}
                        </button>
                    ))}
                </div>

                <div className="detail-tab-panel" role="tabpanel">
                    {activeTab === "description" && (
                        <div>
                            <h3>상품 상세 설명</h3>
                            <p>{description}</p>
                            <ul>
                                <li>핸드메이드 특성상 상품마다 약간의 차이가 있을 수 있습니다.</li>
                                <li>재고와 판매 수량은 주문 흐름에 따라 변경될 수 있습니다.</li>
                            </ul>
                        </div>
                    )}
                    {activeTab === "shipping" && (
                        <div>
                            <h3>배송/교환/반품 안내</h3>
                            <p>주문 후 제작 또는 검수되는 상품은 출고까지 시간이 걸릴 수 있습니다.</p>
                            <p>단순 변심 교환/반품은 상품 수령 후 7일 이내 요청 기준으로 처리합니다.</p>
                        </div>
                    )}
                    {activeTab === "reviews" && (
                        <div>
                            <h3>구매후기</h3>
                            <p>아직 작성된 구매후기가 없습니다. 첫 번째 구매후기를 작성해보세요.</p>
                        </div>
                    )}
                    {activeTab === "qna" && (
                        <div>
                            <h3>Q&A</h3>
                            <p>상품에 대해 궁금한 점이 있으신가요? 질문을 남겨주시면 판매자가 답변해드립니다.</p>
                        </div>
                    )}
                </div>
            </section>

            {/* 담기 성공 후 바로 장바구니로 갈지 계속 볼지 선택하는 모달을 제공함 */}
            {cartModalOpen && (
                <div className="cart-modal-backdrop" role="presentation">
                    <div className="cart-modal" role="dialog" aria-modal="true" aria-labelledby="cart-modal-title">
                        <div className="cart-modal-header">
                            <h3 id="cart-modal-title">장바구니에 담았습니다</h3>
                            <button
                                className="cart-modal-close"
                                type="button"
                                onClick={() => setCartModalOpen(false)}
                                aria-label="모달 닫기"
                            >
                                ×
                            </button>
                        </div>
                        <p>선택한 상품이 장바구니에 추가되었습니다.</p>
                        <div className="cart-modal-actions">
                            <button
                                className="cart-modal-secondary"
                                type="button"
                                onClick={() => setCartModalOpen(false)}
                            >
                                쇼핑 계속하기
                            </button>
                            <Link className="cart-modal-primary" to="/cart">
                                장바구니 보기
                            </Link>
                        </div>
                    </div>
                </div>
            )}
        </section>
    );
}

export default ProductDetailPage;
