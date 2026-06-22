import { useEffect, useState } from "react";
import { Link } from "react-router";
// 내 주문 내역 화면은 주문서 작성 없이 로그인 사용자 주문 목록만 조회함
import { cancelOrder, getMyOrders, updateOrder } from "./orderApi";
import "./OrderListPage.css";

// 주문 수정 폼의 기본 모양을 한 곳에 둬 카드별 입력 fallback으로 사용함
const EMPTY_ORDER_FORM = {
    recipientName: "",
    recipientPhone: "",
    shippingAddress: "",
};

// 서버 enum 값은 유지하고 화면에는 사용자가 읽기 쉬운 한글 상태로 표시함
const ORDER_STATUS_LABELS = {
    PENDING: "배송 대기 중",
    PAID: "결제 완료",
    SHIPPING: "배송 중",
    COMPLETED: "배송 완료",
    CANCELED: "주문 취소",
};

function OrderListPage() {
    // 내 주문 목록 전체를 state로 관리해 수정/취소 후 다시 렌더링함
    const [orders, setOrders] = useState([]);
    // 주문별 수정 폼 값을 orderId 기준 객체로 관리함
    const [editForms, setEditForms] = useState({});
    // 주문 목록만 불러오는 화면이므로 장바구니 로딩과 분리해 단순하게 관리함
    const [loading, setLoading] = useState(true);
    // 주문 목록 쪽 상태 메시지를 별도로 관리함
    const [ordersStatus, setOrdersStatus] = useState({ type: "", text: "" });
    // 수정/취소 중인 작업을 저장해 중복 클릭을 막음
    const [activeAction, setActiveAction] = useState("");

    useEffect(() => {
        let ignore = false;

        async function initializeOrderListPage() {
            try {
                setLoading(true);
                setOrdersStatus({ type: "info", text: "주문 내역을 불러오는 중입니다." });

                const orderData = await getMyOrders();
                if (ignore) {
                    return;
                }

                setOrders(orderData);
                setEditForms(buildEditForms(orderData));
                setOrdersStatus({
                    type: "success",
                    text: `${orderData.length}개의 주문이 있습니다.`,
                });
            } catch (error) {
                console.error(error);
                if (ignore) {
                    return;
                }

                setOrders([]);
                setEditForms({});
                setOrdersStatus({ type: "info", text: "로그인 후 주문 내역을 확인할 수 있습니다." });
            } finally {
                if (!ignore) {
                    setLoading(false);
                }
            }
        }

        // 내 주문 조회 페이지 진입 시 주문 목록만 불러와 주문서 작성 책임과 분리함
        initializeOrderListPage();

        return () => {
            ignore = true;
        };
    }, []);

    // 주문 목록도 배열 여부를 확인해 렌더링 오류를 막음
    const orderItems = Array.isArray(orders) ? orders : [];

    // 주문별 수정 폼 입력을 orderId 기준으로 저장함
    function handleEditFormChange(orderId, event) {
        const { name, value } = event.target;
        setEditForms((currentForms) => ({
            ...currentForms,
            [orderId]: {
                ...currentForms[orderId],
                [name]: value,
            },
        }));
    }

    // 수령 정보 수정은 대기 중인 주문에만 요청하도록 화면에서도 한 번 막음
    async function handleUpdateOrder(order) {
        if (!isPendingOrder(order)) {
            setOrdersStatus({ type: "error", text: "대기 중인 주문만 수정할 수 있습니다." });
            return;
        }

        try {
            setActiveAction(`update:${order.orderId}`);
            setOrdersStatus({ type: "info", text: "수령 정보를 수정하는 중입니다." });

            await updateOrder(order.orderId, editForms[order.orderId]);
            await refreshOrders();
            setOrdersStatus({ type: "success", text: "수령 정보가 수정되었습니다." });
        } catch (error) {
            console.error(error);
            setOrdersStatus({ type: "error", text: getErrorMessage(error, "수령 정보 수정에 실패했습니다.") });
        } finally {
            setActiveAction("");
        }
    }

    // 주문 취소는 실수 방지를 위해 확인 후 대기 중 주문만 취소 요청함
    async function handleCancelOrder(order) {
        if (!isPendingOrder(order)) {
            setOrdersStatus({ type: "error", text: "대기 중인 주문만 취소할 수 있습니다." });
            return;
        }

        if (!window.confirm("주문을 취소하시겠습니까?")) {
            return;
        }

        try {
            setActiveAction(`cancel:${order.orderId}`);
            setOrdersStatus({ type: "info", text: "주문을 취소하는 중입니다." });

            await cancelOrder(order.orderId);
            await refreshOrders();
            setOrdersStatus({ type: "success", text: "주문이 취소되었습니다." });
        } catch (error) {
            console.error(error);
            setOrdersStatus({ type: "error", text: getErrorMessage(error, "주문 취소에 실패했습니다.") });
        } finally {
            setActiveAction("");
        }
    }

    // 수정/취소 후에는 주문 목록만 다시 불러와 카드 상태와 버튼 비활성 조건을 갱신함
    async function refreshOrders() {
        const orderData = await getMyOrders();
        setOrders(orderData);
        setEditForms(buildEditForms(orderData));
    }

    if (loading) {
        return (
            <section className="order-page">
                <div className="order-hero">
                    <h2>내 주문 내역</h2>
                    <p>주문 내역을 불러오는 중입니다.</p>
                </div>
            </section>
        );
    }

    return (
        <section className="order-page">
            <div className="order-hero">
                <div>
                    <h2>내 주문 내역</h2>
                    <p>주문 상태와 수령 정보를 확인하고 대기 중인 주문만 수정하거나 취소할 수 있습니다.</p>
                </div>
                <Link className="order-hero-link" to="/cart">
                    장바구니 보기
                </Link>
            </div>

            <div className="order-list-layout">
                <section className="order-panel">
                    <div className="order-panel-header">
                        <h3>주문 내역</h3>
                        <span>{orderItems.length}개</span>
                    </div>

                    {ordersStatus.text && (
                        <p className={`order-status ${ordersStatus.type}`}>
                            {ordersStatus.text}
                        </p>
                    )}

                    {orderItems.length > 0 ? (
                        <div className="order-list">
                            {orderItems.map((order) => {
                                const editForm = editForms[order.orderId] || EMPTY_ORDER_FORM;
                                const disabled = !isPendingOrder(order);

                                return (
                                    <article className="order-card" key={order.orderId}>
                                        <div className="order-card-head">
                                            <div>
                                                <h4>주문 {order.orderNumber}</h4>
                                                <p>
                                                    {formatDate(order.orderDate)} · {formatPrice(order.totalAmount)}
                                                </p>
                                            </div>
                                            <span className={`order-badge ${order.orderStatus?.toLowerCase() || ""}`}>
                                                {formatOrderStatus(order.orderStatus)}
                                            </span>
                                        </div>

                                        <div className="order-item-list">
                                            {(order.items || []).map((item) => (
                                                <div className="order-item-row" key={item.orderItemId}>
                                                    <span>
                                                        {item.productName} · {item.quantity}개
                                                    </span>
                                                    <strong>{formatPrice(item.lineAmount)}</strong>
                                                </div>
                                            ))}
                                        </div>

                                        <div className="order-edit-form">
                                            <label>
                                                <span>수령인</span>
                                                <input
                                                    name="recipientName"
                                                    value={editForm.recipientName}
                                                    onChange={(event) => handleEditFormChange(order.orderId, event)}
                                                    disabled={disabled}
                                                />
                                            </label>
                                            <label>
                                                <span>연락처</span>
                                                <input
                                                    name="recipientPhone"
                                                    value={editForm.recipientPhone}
                                                    onChange={(event) => handleEditFormChange(order.orderId, event)}
                                                    disabled={disabled}
                                                />
                                            </label>
                                            <label>
                                                <span>배송지</span>
                                                <textarea
                                                    name="shippingAddress"
                                                    value={editForm.shippingAddress}
                                                    onChange={(event) => handleEditFormChange(order.orderId, event)}
                                                    disabled={disabled}
                                                />
                                            </label>
                                        </div>

                                        <div className="order-card-actions">
                                            <button
                                                className="order-light-button"
                                                type="button"
                                                onClick={() => handleUpdateOrder(order)}
                                                disabled={disabled || activeAction === `update:${order.orderId}`}
                                            >
                                                {activeAction === `update:${order.orderId}` ? "수정 중" : "수령 정보 수정"}
                                            </button>
                                            <button
                                                className="order-danger-button"
                                                type="button"
                                                onClick={() => handleCancelOrder(order)}
                                                disabled={disabled || activeAction === `cancel:${order.orderId}`}
                                            >
                                                {activeAction === `cancel:${order.orderId}` ? "취소 중" : "주문 취소"}
                                            </button>
                                        </div>
                                    </article>
                                );
                            })}
                        </div>
                    ) : (
                        <div className="order-empty">
                            <strong>아직 주문이 없습니다.</strong>
                            <Link to="/products">상품 둘러보기</Link>
                        </div>
                    )}
                </section>
            </div>
        </section>
    );
}

// 주문 목록 응답을 수정 폼 state로 변환해 카드별 입력값을 제어함
function buildEditForms(orders) {
    return orders.reduce((forms, order) => ({
        ...forms,
        [order.orderId]: {
            recipientName: order.recipientName || "",
            recipientPhone: order.recipientPhone || "",
            shippingAddress: order.shippingAddress || "",
        },
    }), {});
}

// 대기 중 상태만 사용자 수정/취소가 가능하므로 버튼 조건을 함수로 분리함
function isPendingOrder(order) {
    return order.orderStatus === "PENDING";
}

// 금액 표기를 상품/장바구니 화면과 같은 한국어 원화 형식으로 맞춤
function formatPrice(value) {
    return `${Number(value || 0).toLocaleString("ko-KR")}원`;
}

// LocalDateTime이 문자열 또는 배열로 내려와도 화면에서 같은 날짜 형식으로 보이게 처리함
function formatDate(value) {
    if (!value) {
        return "-";
    }

    const date = Array.isArray(value)
        ? new Date(value[0], value[1] - 1, value[2], value[3] || 0, value[4] || 0)
        : new Date(value);

    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString("ko-KR");
}

// 서버 상태값이 추가돼도 화면이 비지 않게 원본 값을 fallback으로 보여줌
function formatOrderStatus(status) {
    return ORDER_STATUS_LABELS[status] || status || "-";
}

// Axios 오류 응답에서 서버 메시지를 최대한 꺼내 사용자에게 보여줌
function getErrorMessage(error, fallbackMessage) {
    const responseMessage = error?.response?.data?.message || error?.response?.data;
    return typeof responseMessage === "string" && responseMessage.trim()
        ? responseMessage
        : fallbackMessage;
}

export default OrderListPage;
