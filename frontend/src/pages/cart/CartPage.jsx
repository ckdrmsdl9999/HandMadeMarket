import { useEffect, useState } from "react";
import { Link } from "react-router";
// 장바구니는 현재 로그인 사용자를 기준으로만 조회하므로 사용자 API를 먼저 호출함
import { getCurrentUser } from "../auth/authApi";
// 장바구니 조회/수정/삭제 API를 화면 이벤트에서 재사용함
import {
    clearCartItems,
    getCart,
    removeCartItem,
    updateCartItemQuantity,
} from "./cartApi";
import "./CartPage.css";

// 빈 장바구니 모양을 한 곳에 둬 비로그인/조회 실패/전체 삭제 후 화면을 같은 구조로 맞춤
const EMPTY_CART = {
    userId: null,
    totalAmount: 0,
    cartItems: [],
};

function CartPage() {
    // 서버에서 받은 장바구니 응답 전체를 state로 보관해 요약과 목록이 같은 데이터를 보게 함
    const [cart, setCart] = useState(EMPTY_CART);
    // 현재 로그인 사용자 정보를 요약 카드와 API 경로에 사용함
    const [currentUser, setCurrentUser] = useState(null);
    // 초기 장바구니 조회 중인지 구분해 로딩 안내를 보여줌
    const [loading, setLoading] = useState(true);
    // 성공/실패 안내 문구를 한 위치에서 관리함
    const [statusMessage, setStatusMessage] = useState({ type: "", text: "" });
    // 어떤 항목이 수정/삭제 중인지 저장해 중복 클릭을 막음
    const [activeAction, setActiveAction] = useState("");

    useEffect(() => {
        let ignore = false;

        async function fetchCart() {
            try {
                setLoading(true);
                setStatusMessage({ type: "", text: "" });

                const user = await getCurrentUser();
                if (!user?.id) {
                    throw new Error("LOGIN_REQUIRED");
                }

                const cartData = await getCart(user.id);
                if (ignore) {
                    return;
                }

                setCurrentUser(user);
                setCart(cartData);
                setStatusMessage({ type: "success", text: "장바구니를 불러왔습니다." });
            } catch (error) {
                console.error(error);
                if (ignore) {
                    return;
                }

                setCurrentUser(null);
                setCart(EMPTY_CART);
                setStatusMessage({
                    type: "info",
                    text: "로그인 후 장바구니를 확인할 수 있습니다.",
                });
            } finally {
                if (!ignore) {
                    setLoading(false);
                }
            }
        }

        // 장바구니 페이지 진입 시 현재 로그인 사용자의 장바구니를 자동으로 조회함
        fetchCart();

        return () => {
            ignore = true;
        };
    }, []);

    // 서버에서 내려온 cartItems가 없을 때도 안전하게 빈 배열로 처리함
    const cartItems = Array.isArray(cart.cartItems) ? cart.cartItems : [];
    // 요약 카드에서 상품 종류 수를 바로 보여주기 위해 계산 값을 분리함
    const itemCount = cartItems.length;
    // 버튼 비활성 조건을 한 값으로 묶어 화면 곳곳에서 같은 기준을 사용함
    const hasCartItems = itemCount > 0;

    // 수량 변경은 서버 응답을 다시 cart state에 반영해 총액까지 함께 갱신함
    async function handleQuantityChange(cartItemId, nextQuantity) {
        if (!currentUser?.id) {
            setStatusMessage({ type: "error", text: "로그인 후 장바구니를 수정할 수 있습니다." });
            return;
        }

        const parsedQuantity = Math.max(1, Number(nextQuantity) || 1);

        try {
            setActiveAction(`quantity:${cartItemId}`);
            const updatedCart = await updateCartItemQuantity(currentUser.id, cartItemId, parsedQuantity);
            setCart(updatedCart);
            setStatusMessage({ type: "success", text: "수량을 변경했습니다." });
        } catch (error) {
            console.error(error);
            setStatusMessage({ type: "error", text: "수량 변경에 실패했습니다." });
        } finally {
            setActiveAction("");
        }
    }

    // 항목 삭제는 서버 응답을 기준으로 목록과 요약을 다시 그림
    async function handleRemoveItem(cartItemId) {
        if (!currentUser?.id) {
            setStatusMessage({ type: "error", text: "로그인 후 장바구니를 수정할 수 있습니다." });
            return;
        }

        try {
            setActiveAction(`remove:${cartItemId}`);
            const updatedCart = await removeCartItem(currentUser.id, cartItemId);
            setCart(updatedCart);
            setStatusMessage({ type: "success", text: "항목을 삭제했습니다." });
        } catch (error) {
            console.error(error);
            setStatusMessage({ type: "error", text: "항목 삭제에 실패했습니다." });
        } finally {
            setActiveAction("");
        }
    }

    // 전체 비우기는 실수 방지를 위해 확인 후 서버에 삭제 요청을 보냄
    async function handleClearCart() {
        if (!currentUser?.id) {
            setStatusMessage({ type: "error", text: "로그인 후 장바구니를 비울 수 있습니다." });
            return;
        }

        if (!window.confirm("장바구니를 모두 비우시겠습니까?")) {
            return;
        }

        try {
            setActiveAction("clear");
            const updatedCart = await clearCartItems(currentUser.id);
            setCart(updatedCart);
            setStatusMessage({ type: "success", text: "장바구니를 비웠습니다." });
        } catch (error) {
            console.error(error);
            setStatusMessage({ type: "error", text: "장바구니 비우기에 실패했습니다." });
        } finally {
            setActiveAction("");
        }
    }

    if (loading) {
        return (
            <section className="cart-page">
                <div className="cart-hero">
                    <h2>장바구니</h2>
                    <p>장바구니를 불러오는 중입니다.</p>
                </div>
            </section>
        );
    }

    return (
        <section className="cart-page">
            <div className="cart-hero">
                <div>
                    <h2>장바구니</h2>
                    <p>
                        {currentUser
                            ? "현재 로그인한 계정의 장바구니입니다."
                            : "로그인 후 내 장바구니를 확인할 수 있습니다."}
                    </p>
                </div>
                <Link className="cart-continue-link" to="/products">
                    상품 계속 보기
                </Link>
            </div>

            <section className="cart-summary">
                <div className="cart-summary-card">
                    <span>사용자</span>
                    <strong>{currentUser?.userName || currentUser?.loginId || "-"}</strong>
                </div>
                <div className="cart-summary-card">
                    <span>상품 종류</span>
                    <strong>{itemCount}개</strong>
                </div>
                <div className="cart-summary-card">
                    <span>총 결제 금액</span>
                    <strong>{formatPrice(cart.totalAmount)}</strong>
                </div>
                {hasCartItems ? (
                    <Link className="cart-order-button" to="/orders">
                        주문하기
                    </Link>
                ) : (
                    <button className="cart-order-button" type="button" disabled>
                        주문하기
                    </button>
                )}
                <button
                    className="cart-clear-button"
                    type="button"
                    onClick={handleClearCart}
                    disabled={!hasCartItems || activeAction === "clear"}
                >
                    {activeAction === "clear" ? "비우는 중" : "장바구니 비우기"}
                </button>
            </section>

            {statusMessage.text && (
                <p className={`cart-status ${statusMessage.type}`}>
                    {statusMessage.text}
                </p>
            )}

            <section className="cart-panel">
                {hasCartItems ? (
                    <div className="cart-table-wrap">
                        <table className="cart-table">
                            <thead>
                                <tr>
                                    <th>상품명</th>
                                    <th>단가</th>
                                    <th>수량</th>
                                    <th>합계</th>
                                    <th>작업</th>
                                </tr>
                            </thead>
                            <tbody>
                                {cartItems.map((item) => (
                                    <tr key={item.cartItemId}>
                                        <td>
                                            <Link className="cart-product-link" to={`/products/${item.productId}`}>
                                                {item.productNameSnapshot}
                                            </Link>
                                            <small>#{item.productId}</small>
                                        </td>
                                        <td>{formatPrice(item.unitPriceSnapshot)}</td>
                                        <td>
                                            <div className="cart-quantity-control">
                                                <button
                                                    type="button"
                                                    onClick={() => handleQuantityChange(item.cartItemId, item.quantity - 1)}
                                                    disabled={item.quantity <= 1 || activeAction === `quantity:${item.cartItemId}`}
                                                >
                                                    -
                                                </button>
                                                <span>{item.quantity}</span>
                                                <button
                                                    type="button"
                                                    onClick={() => handleQuantityChange(item.cartItemId, item.quantity + 1)}
                                                    disabled={activeAction === `quantity:${item.cartItemId}`}
                                                >
                                                    +
                                                </button>
                                            </div>
                                        </td>
                                        <td>
                                            <strong>{formatPrice(item.lineAmount)}</strong>
                                        </td>
                                        <td>
                                            <button
                                                className="cart-remove-button"
                                                type="button"
                                                onClick={() => handleRemoveItem(item.cartItemId)}
                                                disabled={activeAction === `remove:${item.cartItemId}`}
                                            >
                                                {activeAction === `remove:${item.cartItemId}` ? "삭제 중" : "삭제"}
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <div className="cart-empty">
                        <strong>장바구니가 비어 있습니다.</strong>
                        <p>마음에 드는 핸드메이드 상품을 담아보세요.</p>
                        <Link to="/products">상품 둘러보기</Link>
                    </div>
                )}
            </section>
        </section>
    );
}

// 장바구니 금액 표기를 한국어 원화 형식으로 통일함
function formatPrice(value) {
    return `${Number(value || 0).toLocaleString("ko-KR")}원`;
}

export default CartPage;
