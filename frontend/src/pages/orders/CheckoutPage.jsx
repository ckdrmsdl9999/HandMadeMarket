import { useEffect, useState } from "react";
import { Link } from "react-router";
// 주문서 화면은 현재 로그인 사용자 정보를 수령인 기본값에 사용함
import { getCurrentUser } from "../auth/authApi";
// 주문서에서 상품 확인과 개별 제거를 같이 처리하도록 장바구니 API를 가져옴
import { getCart, removeCartItem } from "../cart/cartApi";
// 주문 생성 API만 가져와 주문내역 조회 화면과 책임을 분리함
import { createOrder } from "./orderApi";
import "./OrderListPage.css";

// 주문 생성 폼의 기본 모양을 한 곳에 둬 초기화와 제출 후 리셋에 같이 사용함
const EMPTY_ORDER_FORM = {
    recipientName: "",
    recipientPhone: "",
    shippingAddress: "",
};

// 장바구니가 비었거나 조회 실패했을 때도 같은 데이터 구조로 화면을 그리게 함
const EMPTY_CART = {
    totalAmount: 0,
    cartItems: [],
};

function CheckoutPage() {
    // 현재 사용자 정보를 주문서 기본 수령인과 장바구니 조회에 사용함
    const [currentUser, setCurrentUser] = useState(null);
    // 주문 생성 전 확인할 장바구니 요약을 보관함
    const [cart, setCart] = useState(EMPTY_CART);
    // 주문 생성 폼 입력값을 React state와 연결함
    const [orderForm, setOrderForm] = useState(EMPTY_ORDER_FORM);
    // 주문서와 장바구니 정보를 불러오는 동안 로딩 화면을 보여줌
    const [loading, setLoading] = useState(true);
    // 주문 생성 상태 메시지를 주문서 화면 안에서만 관리함
    const [orderStatus, setOrderStatus] = useState({ type: "", text: "" });
    // 주문 생성 중 중복 클릭을 막기 위해 처리 상태를 저장함
    const [activeAction, setActiveAction] = useState("");

    useEffect(() => {
        let ignore = false;

        async function initializeCheckoutPage() {
            try {
                setLoading(true);
                setOrderStatus({ type: "info", text: "주문서를 불러오는 중입니다." });

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
                setOrderForm(buildOrderFormForUser(user));
                setOrderStatus({ type: "", text: "" });
            } catch (error) {
                console.error(error);
                if (ignore) {
                    return;
                }

                setCurrentUser(null);
                setCart(EMPTY_CART);
                setOrderForm(EMPTY_ORDER_FORM);
                setOrderStatus({ type: "info", text: "로그인 후 주문서를 작성할 수 있습니다." });
            } finally {
                if (!ignore) {
                    setLoading(false);
                }
            }
        }

        // 주문서 페이지 진입 시 장바구니만 불러와 내 주문 목록 조회와 분리함
        initializeCheckoutPage();

        return () => {
            ignore = true;
        };
    }, []);

    // 서버 응답이 비어도 안전하게 빈 배열로 처리함
    const cartItems = Array.isArray(cart.cartItems) ? cart.cartItems : [];
    // 장바구니 상품이 있을 때만 주문 생성 버튼을 활성화함
    const hasCartItems = cartItems.length > 0;
    // 주문은 한 판매자 상품만 허용하므로 주문서에서도 판매자 혼합 여부를 계산함
    const hasMultipleSellers = countDistinctSellers(cartItems) > 1;
    // 주문 생성 버튼 조건을 한 값으로 묶어 화면 표시와 submit 방어가 같은 기준을 쓰게 함
    const canCreateOrder = currentUser && hasCartItems && !hasMultipleSellers;

    // 주문 생성 폼 입력을 state에 반영해 제출 payload와 화면 값을 같게 유지함
    function handleOrderFormChange(event) {
        const { name, value } = event.target;
        setOrderForm((currentForm) => ({
            ...currentForm,
            [name]: value,
        }));
    }

    // 주문 생성은 서버가 현재 장바구니 기준으로 처리하므로 수령 정보만 보냄
    async function handleCreateOrder(event) {
        event.preventDefault();

        if (!currentUser?.id) {
            setOrderStatus({ type: "error", text: "로그인 후 주문할 수 있습니다." });
            return;
        }

        if (!hasCartItems) {
            setOrderStatus({ type: "error", text: "장바구니가 비어 있습니다." });
            return;
        }

        if (hasMultipleSellers) {
            setOrderStatus({ type: "error", text: "서로 다른 판매자의 상품은 한 번에 주문할 수 없습니다." });
            return;
        }

        try {
            setActiveAction("create");
            setOrderStatus({ type: "info", text: "주문을 생성하는 중입니다." });

            const result = await createOrder(orderForm);
            setOrderStatus({
                type: "success",
                text: `주문이 생성되었습니다. 주문 ID: ${result.orderId}`,
            });
            await refreshCheckoutPage(currentUser.id);
            setOrderForm(buildOrderFormForUser(currentUser));
        } catch (error) {
            console.error(error);
            setOrderStatus({ type: "error", text: getErrorMessage(error, "주문 생성에 실패했습니다.") });
        } finally {
            setActiveAction("");
        }
    }

    // 주문서에서 판매자가 섞인 상품을 바로 뺄 수 있도록 장바구니 항목 삭제를 연결함
    async function handleRemoveCartItem(cartItemId) {
        if (!currentUser?.id) {
            setOrderStatus({ type: "error", text: "로그인 후 주문 상품을 뺄 수 있습니다." });
            return;
        }

        try {
            setActiveAction(`remove:${cartItemId}`);
            setOrderStatus({ type: "info", text: "주문 상품에서 빼는 중입니다." });

            const updatedCart = await removeCartItem(currentUser.id, cartItemId);
            setCart(updatedCart);
            setOrderStatus({ type: "success", text: "주문 상품에서 뺐습니다." });
        } catch (error) {
            console.error(error);
            setOrderStatus({ type: "error", text: getErrorMessage(error, "주문 상품을 빼지 못했습니다.") });
        } finally {
            setActiveAction("");
        }
    }

    // 주문 생성 후 장바구니를 다시 불러와 비워진 상태와 총액을 화면에 반영함
    async function refreshCheckoutPage(userId) {
        const cartData = await getCart(userId);
        setCart(cartData);
    }

    if (loading) {
        return (
            <section className="order-page">
                <div className="order-hero">
                    <h2>주문서 작성</h2>
                    <p>주문서를 불러오는 중입니다.</p>
                </div>
            </section>
        );
    }

    return (
        <section className="order-page">
            <div className="order-hero">
                <div>
                    <h2>주문서 작성</h2>
                    <p>
                        {currentUser
                            ? "장바구니 상품을 확인하고 수령 정보를 입력해 주문합니다."
                            : "로그인 후 장바구니 상품으로 주문서를 작성할 수 있습니다."}
                    </p>
                </div>
                <Link className="order-hero-link" to="/orders">
                    주문 내역 보기
                </Link>
            </div>

            <div className="checkout-layout">
                <aside className="order-panel checkout-panel">
                    <div className="order-panel-header">
                        <h3>주문 상품</h3>
                        <span>{currentUser?.userName || currentUser?.loginId || "-"}</span>
                    </div>

                    <div className="order-cart-list">
                        {hasMultipleSellers && (
                            // 서로 다른 판매자 상품이 섞였을 때 주문 생성 전에 이유를 바로 보여줌
                            <p className="order-seller-warning">
                                서로 다른 판매자의 상품은 한 번에 주문할 수 없습니다.
                            </p>
                        )}
                        {hasCartItems ? (
                            cartItems.map((item) => (
                                <div className="order-cart-item" key={item.cartItemId}>
                                    <div>
                                        <strong>{item.productNameSnapshot}</strong>
                                        {/* 주문 전 상품별 판매자를 확인해 서로 다른 판매자 상품 여부를 화면에서 알 수 있게 함 */}
                                        <span className="order-cart-seller">
                                            판매자: {formatSellerName(item.sellerName)}
                                        </span>
                                        <span>
                                            {formatPrice(item.unitPriceSnapshot)} x {item.quantity}개
                                        </span>
                                    </div>
                                    <div className="order-cart-item-side">
                                        <strong>{formatPrice(item.lineAmount)}</strong>
                                        {/* 주문서 상품 카드에서 불필요한 장바구니 항목을 바로 제거하게 함 */}
                                        <button
                                            className="order-cart-remove-button"
                                            type="button"
                                            onClick={() => handleRemoveCartItem(item.cartItemId)}
                                            disabled={activeAction === "create" || activeAction === `remove:${item.cartItemId}`}
                                        >
                                            {activeAction === `remove:${item.cartItemId}` ? "빼는 중" : "빼기"}
                                        </button>
                                    </div>
                                </div>
                            ))
                        ) : (
                            <div className="order-empty">
                                <strong>장바구니가 비어 있습니다.</strong>
                                <Link to="/products">상품 둘러보기</Link>
                            </div>
                        )}
                    </div>

                    <div className="order-total">
                        <span>총 결제 금액</span>
                        <strong>{formatPrice(cart.totalAmount)}</strong>
                    </div>
                </aside>

                <section className="order-panel checkout-panel">
                    <div className="order-panel-header">
                        <h3>수령 정보</h3>
                    </div>

                    <form className="order-form" onSubmit={handleCreateOrder}>
                        <label>
                            <span>수령인</span>
                            <input
                                name="recipientName"
                                value={orderForm.recipientName}
                                onChange={handleOrderFormChange}
                                required
                                disabled={!currentUser}
                            />
                        </label>
                        <label>
                            <span>연락처</span>
                            <input
                                name="recipientPhone"
                                value={orderForm.recipientPhone}
                                onChange={handleOrderFormChange}
                                placeholder="010-0000-0000"
                                required
                                disabled={!currentUser}
                            />
                        </label>
                        <label>
                            <span>배송지</span>
                            <textarea
                                name="shippingAddress"
                                value={orderForm.shippingAddress}
                                onChange={handleOrderFormChange}
                                placeholder="배송 받을 주소를 입력하세요"
                                required
                                disabled={!currentUser}
                            />
                        </label>
                        <button
                            className="order-submit-button"
                            type="submit"
                            disabled={!canCreateOrder || activeAction === "create"}
                        >
                            {activeAction === "create" ? "주문 생성 중" : "주문하기"}
                        </button>
                    </form>

                    {orderStatus.text && (
                        <p className={`order-status ${orderStatus.type}`}>
                            {orderStatus.text}
                        </p>
                    )}
                </section>
            </div>
        </section>
    );
}

// 현재 사용자 이름을 주문서 수령인 기본값으로 넣어 주문서 첫 입력 부담을 줄임
function buildOrderFormForUser(user) {
    return {
        ...EMPTY_ORDER_FORM,
        recipientName: user?.userName || user?.loginId || "",
    };
}

// 금액 표기를 상품/장바구니 화면과 같은 한국어 원화 형식으로 맞춤
function formatPrice(value) {
    return `${Number(value || 0).toLocaleString("ko-KR")}원`;
}

// 판매자명이 없는 응답도 주문서 카드에서 빈 값으로 보이지 않게 처리함
function formatSellerName(value) {
    return value || "알 수 없음";
}

// 판매자 ID 기준으로 서로 다른 판매자가 섞였는지 계산함
function countDistinctSellers(cartItems) {
    return new Set(
        cartItems
            .map((item) => item.sellerId)
            .filter((sellerId) => sellerId !== null && sellerId !== undefined)
    ).size;
}

// Axios 오류 응답에서 서버 메시지를 최대한 꺼내 사용자에게 보여줌
function getErrorMessage(error, fallbackMessage) {
    const responseMessage = error?.response?.data?.message || error?.response?.data;
    return typeof responseMessage === "string" && responseMessage.trim()
        ? responseMessage
        : fallbackMessage;
}

export default CheckoutPage;
