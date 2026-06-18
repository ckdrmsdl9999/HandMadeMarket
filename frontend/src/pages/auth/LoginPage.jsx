import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router";
// 로그인 화면에서 현재 세션 확인, 로컬 로그인, 로그아웃, 백엔드 이동 링크를 함께 사용함
import {
    getCurrentUser,
    getOAuthLoginUrl,
    getServerPageUrl,
    logoutUser,
    signIn,
} from "./authApi";
import "./LoginPage.css";

// 로그인 폼 입력값의 초기 모양을 한 곳에 둬 초기화와 state 생성에 같이 사용함
const EMPTY_LOGIN_FORM = {
    loginId: "",
    password: "",
};

function LoginPage() {
    // 아이디/비밀번호 입력값을 React state로 관리함
    const [loginForm, setLoginForm] = useState(EMPTY_LOGIN_FORM);
    // 이미 로그인된 사용자가 있으면 로그인 폼 대신 계정 연결 상태를 보여줌
    const [currentUser, setCurrentUser] = useState(null);
    // 세션 확인 중인지 구분해 초기 로딩 안내를 보여줌
    const [loading, setLoading] = useState(true);
    // 로그인 성공/실패 안내를 한 위치에서 관리함
    const [loginMessage, setLoginMessage] = useState({ type: "", text: "" });
    // 로그인/로그아웃 중복 클릭을 막기 위해 진행 중인 작업을 저장함
    const [activeAction, setActiveAction] = useState("");
    // OAuth 실패 쿼리와 로그인 후 이동 경로를 읽기 위해 searchParams를 사용함
    const [searchParams] = useSearchParams();
    // 로그인 성공 후 React 라우트 안에서 이동하기 위해 navigate를 사용함
    const navigate = useNavigate();

    useEffect(() => {
        let ignore = false;

        async function checkCurrentSession() {
            try {
                setLoading(true);
                const user = await getCurrentUser();
                if (ignore) {
                    return;
                }

                setCurrentUser(user);
                setLoginMessage({ type: "success", text: "이미 로그인되어 있습니다." });
            } catch {
                if (ignore) {
                    return;
                }

                setCurrentUser(null);
                setLoginMessage(resolveInitialLoginMessage(searchParams.get("oauthError")));
            } finally {
                if (!ignore) {
                    setLoading(false);
                }
            }
        }

        // 로그인 페이지 진입 시 현재 세션을 확인해 폼 또는 로그인 상태를 나눠 보여줌
        checkCurrentSession();

        return () => {
            ignore = true;
        };
    }, [searchParams]);

    // 로컬 로그인 입력값을 state에 반영해 제출 payload와 화면 값을 같게 유지함
    function handleLoginFormChange(event) {
        const { name, value } = event.target;
        setLoginForm((currentForm) => ({
            ...currentForm,
            [name]: value,
        }));
    }

    // 아이디 로그인은 signin API로 세션을 만든 뒤 현재 사용자 정보를 다시 확인함
    async function handleLocalLogin(event) {
        event.preventDefault();

        try {
            setActiveAction("login");
            setLoginMessage({ type: "info", text: "로그인 중입니다." });

            await signIn(loginForm);
            const user = await getCurrentUser();
            setCurrentUser(user);
            setLoginForm(EMPTY_LOGIN_FORM);
            setLoginMessage({ type: "success", text: "로그인되었습니다." });
            navigate(searchParams.get("redirect") || "/", { replace: true });
        } catch (error) {
            console.error(error);
            setLoginMessage({ type: "error", text: getErrorMessage(error, "아이디 또는 비밀번호를 확인해 주세요.") });
        } finally {
            setActiveAction("");
        }
    }

    // 로그인 화면에서도 바로 로그아웃할 수 있게 세션 종료 API를 연결함
    async function handleLogout() {
        try {
            setActiveAction("logout");
            await logoutUser();
            setCurrentUser(null);
            setLoginMessage({ type: "success", text: "로그아웃되었습니다." });
        } catch (error) {
            console.error(error);
            setLoginMessage({ type: "error", text: getErrorMessage(error, "로그아웃에 실패했습니다.") });
        } finally {
            setActiveAction("");
        }
    }

    if (loading) {
        return (
            <section className="login-page">
                <div className="login-hero">
                    <h2>계정 로그인</h2>
                    <p>로그인 상태를 확인하는 중입니다.</p>
                </div>
            </section>
        );
    }

    return (
        <section className="login-page">
            <div className="login-layout">
                <section className="login-hero">
                    <p className="login-eyebrow">LOGIN CENTER</p>
                    <h2>
                        로그인하고
                        <br />
                        나만의 마켓을 이어가세요
                    </h2>
                    <p>장바구니, 주문 내역, 계정 정보를 한 곳에서 확인할 수 있습니다.</p>
                </section>

                <section className="login-panel">
                    <div className="login-panel-header">
                        <div>
                            <p className="login-eyebrow">ACCOUNT</p>
                            <h3>계정 로그인</h3>
                        </div>
                        <Link to="/">홈으로</Link>
                    </div>

                    {currentUser ? (
                        <div className="login-profile-box">
                            <strong>{currentUser.userName || currentUser.loginId}님 계정이 연결되어 있습니다.</strong>
                            <dl>
                                <div>
                                    <dt>회원 유형</dt>
                                    <dd>{formatProvider(currentUser.provider)}</dd>
                                </div>
                                <div>
                                    <dt>권한</dt>
                                    <dd>{currentUser.role || "-"}</dd>
                                </div>
                                <div>
                                    <dt>이메일</dt>
                                    <dd>{currentUser.email || "등록된 이메일 없음"}</dd>
                                </div>
                            </dl>
                            <div className="login-profile-actions">
                                <Link className="login-primary-link" to="/orders">주문 내역</Link>
                                <Link className="login-secondary-link" to="/cart">장바구니</Link>
                                <button
                                    className="login-secondary-link"
                                    type="button"
                                    onClick={handleLogout}
                                    disabled={activeAction === "logout"}
                                >
                                    {activeAction === "logout" ? "로그아웃 중" : "로그아웃"}
                                </button>
                            </div>
                        </div>
                    ) : (
                        <>
                            <p className="login-description">
                                아이디와 비밀번호로 로그인하거나 간편 로그인을 이용할 수 있습니다.
                                로그인 후 장바구니와 주문 내역을 이어서 확인할 수 있습니다.
                            </p>

                            <a className="login-social-button naver" href={getOAuthLoginUrl("naver")}>
                                네이버로 로그인
                            </a>
                            <a className="login-social-button google" href={getOAuthLoginUrl("google")}>
                                구글로 로그인
                            </a>

                            <div className="login-divider">또는</div>

                            <form className="login-form" onSubmit={handleLocalLogin}>
                                <label>
                                    <span>아이디</span>
                                    <input
                                        name="loginId"
                                        type="text"
                                        value={loginForm.loginId}
                                        onChange={handleLoginFormChange}
                                        autoComplete="username"
                                        required
                                    />
                                </label>
                                <label>
                                    <span>비밀번호</span>
                                    <input
                                        name="password"
                                        type="password"
                                        value={loginForm.password}
                                        onChange={handleLoginFormChange}
                                        autoComplete="current-password"
                                        required
                                    />
                                </label>
                                <button
                                    className="login-submit-button"
                                    type="submit"
                                    disabled={activeAction === "login"}
                                >
                                    {activeAction === "login" ? "로그인 중" : "아이디로 로그인"}
                                </button>
                            </form>

                            <a className="login-signup-link" href={getServerPageUrl("/signup")}>
                                회원가입
                            </a>
                        </>
                    )}

                    {loginMessage.text && (
                        <p className={`login-message ${loginMessage.type}`}>
                            {loginMessage.text}
                        </p>
                    )}
                </section>
            </div>
        </section>
    );
}

// OAuth 실패 쿼리를 사용자가 읽을 수 있는 로그인 안내 문구로 변환함
function resolveInitialLoginMessage(oauthError) {
    if (oauthError === "account_deleted") {
        return { type: "error", text: "탈퇴한 이력이 있는 아이디입니다." };
    }

    if (oauthError) {
        return { type: "error", text: "소셜 로그인에 실패했습니다." };
    }

    return { type: "info", text: "현재 비로그인 상태입니다." };
}

// provider 원본값을 로그인 화면에 표시할 회원 유형 이름으로 바꿈
function formatProvider(provider) {
    if (provider === "local") {
        return "일반 회원";
    }

    if (provider === "naver") {
        return "네이버 회원";
    }

    if (provider === "google") {
        return "구글 회원";
    }

    return provider || "-";
}

// Axios 오류 응답의 message 필드를 우선 사용해 백엔드 검증 메시지를 그대로 보여줌
function getErrorMessage(error, fallbackMessage) {
    const responseMessage = error?.response?.data?.message || error?.response?.data;
    return typeof responseMessage === "string" && responseMessage.trim()
        ? responseMessage
        : fallbackMessage;
}

export default LoginPage;
