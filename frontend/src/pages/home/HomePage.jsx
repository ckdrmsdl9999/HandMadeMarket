import { useEffect, useState } from "react";
import { Link } from "react-router";
// 홈에서도 로그인 상태와 백엔드 이동 링크를 같은 인증 API 기준으로 맞춤
import { getCurrentUser, getOAuthLoginUrl, getServerPageUrl } from "../auth/authApi";
// 홈 추천 상품은 인기 상품 API를 우선 사용하고 비어 있으면 전체 상품으로 보완함
import { getPopularProducts, getProducts } from "../products/productApi";
// 상품 카드의 이미지와 가격 표시는 상품 목록 화면과 같은 규칙을 사용함
import { formatPrice, isImageUrl } from "../products/productDisplay";
import "./HomePage.css";

// CloudFront 기본 주소가 없으면 빈 값으로 두어 기존 그라데이션 배너를 유지함
const HOME_IMAGE_BASE_URL = normalizeHomeImageBaseUrl(import.meta.env.VITE_CLOUDFRONT_IMAGE_BASE_URL);

// 홈 빠른 카테고리는 실제 상품 목록 필터와 같은 값만 노출함
const CATEGORY_LINKS = [
    { label: "비누", description: "수제 비누와 생활용품" },
    { label: "목공", description: "원목 소품과 트레이" },
    { label: "패브릭", description: "천 가방과 니트 제품" },
    { label: "문구", description: "감성 문구와 선물" },
];

// 홈 배너 문구는 기존 Thymeleaf 홈의 쇼핑 진입 흐름과 맞춤
const HERO_BANNERS = [
    {
        title: "오늘 단 하루 특가(배너상품) ",
        description: "장인이 만든 상품입니다.",
        // CloudFront 설정 후 같은 경로에 이미지를 올리면 홈 배너에 바로 표시함
        imageUrl: getHomeImageUrl("home/hero-shopping.jpg"),
        to: "/products",
        action: "상품 보러가기",
        tone: "primary",
    },
    {
        title: "첫 구매는 로그인 후 이어가기",
        description: "장바구니와 주문 내역을 계정에 저장해 편하게 관리할 수 있습니다.",
        // 로그인 배너도 CloudFront 이미지가 없으면 기존 색상 배너로 fallback함
        imageUrl: getHomeImageUrl("home/hero-login.jpg"),
        to: "/login",
        action: "로그인하기",
        tone: "accent",
    },
];

// 홈 추천 영역은 타임리프 카드 수와 비슷하게 최대 5개만 표시함
const FEATURED_PRODUCT_LIMIT = 5;

function HomePage() {
    const [currentUser, setCurrentUser] = useState(null);
    const [featuredProducts, setFeaturedProducts] = useState([]);
    const [loadingProducts, setLoadingProducts] = useState(true);
    const [productMessage, setProductMessage] = useState("");

    useEffect(() => {
        let ignore = false;

        async function fetchHomeData() {
            setLoadingProducts(true);
            setProductMessage("");

            // 사용자 조회 실패가 추천 상품 로딩을 막지 않게 두 요청을 독립 결과로 처리함
            const [userResult, popularProductResult] = await Promise.allSettled([
                getCurrentUser(),
                getPopularProducts(FEATURED_PRODUCT_LIMIT),
            ]);

            if (ignore) {
                return;
            }

            setCurrentUser(userResult.status === "fulfilled" ? userResult.value : null);

            const popularProducts = normalizeProducts(popularProductResult);
            if (popularProducts.length > 0) {
                setFeaturedProducts(popularProducts);
                setLoadingProducts(false);
                return;
            }

            try {
                // 인기 상품이 없거나 실패하면 전체 상품 일부를 보여 홈 카드 공백을 줄임
                const products = await getProducts();
                if (!ignore) {
                    setFeaturedProducts(selectFeaturedProducts(products));
                }
            } catch (error) {
                console.error(error);
                if (!ignore) {
                    setFeaturedProducts([]);
                    setProductMessage("추천 상품을 불러오지 못했습니다.");
                }
            } finally {
                if (!ignore) {
                    setLoadingProducts(false);
                }
            }
        }

        fetchHomeData();

        return () => {
            ignore = true;
        };
    }, []);

    const displayName = getDisplayName(currentUser);
    const canManageProducts = hasRole(currentUser, "SELLER") || hasRole(currentUser, "ADMIN");
    const canManageAdmin = hasRole(currentUser, "ADMIN");

    return (
        <section className="home-page">
            <section className="home-hero-grid">
                <aside className="home-category-panel">
                    <h2>빠른 카테고리</h2>
                    <div className="home-category-list">
                        {CATEGORY_LINKS.map((category) => (
                            // 카테고리 클릭 시 상품 목록 query로 이동해 기존 목록 필터를 그대로 사용함
                            <Link
                                className="home-category-link"
                                key={category.label}
                                to={`/products?category=${encodeURIComponent(category.label)}`}
                            >
                                <strong>{category.label}</strong>
                                <span>{category.description}</span>
                            </Link>
                        ))}
                    </div>
                </aside>

                <div className="home-banner-stack">
                    {HERO_BANNERS.map((banner) => (
                        // 이미지 URL이 있을 때만 배너 이미지를 얹어 CloudFront 설정 전 fallback을 유지함
                        <article
                            className={`home-banner ${banner.tone} ${banner.imageUrl ? "has-image" : ""}`}
                            key={banner.title}
                        >
                            {banner.imageUrl && (
                                <img className="home-banner-image" src={banner.imageUrl} alt="" />
                            )}
                            <div className="home-banner-content">
                                <strong>{banner.title}</strong>
                                <p>{banner.description}</p>
                            </div>
                            <Link to={banner.to}>{banner.action}</Link>
                        </article>
                    ))}
                </div>

                {currentUser ? (
                    <aside className="home-account-panel">
                        <p className="home-panel-eyebrow">MY MARKET</p>
                        <h2>{displayName}님</h2>
                        <p>현재 로그인된 계정으로 장바구니와 주문 내역을 이어서 확인할 수 있습니다.</p>

                        <div className="home-account-actions">
                            <Link className="home-primary-button" to="/orders">
                                주문내역 보기
                            </Link>
                            <Link className="home-secondary-button" to="/cart">
                                장바구니 보기
                            </Link>
                            {canManageProducts && (
                                // 판매자와 관리자만 백엔드 상품 등록 화면으로 이동할 수 있게 노출함
                                <a className="home-secondary-button" href={getServerPageUrl("/seller/products/new")}>
                                    상품 등록
                                </a>
                            )}
                            {canManageAdmin && (
                                // 관리자 전용 Thymeleaf 관리 화면은 아직 React가 아니므로 백엔드 링크로 연결함
                                <a className="home-secondary-button" href={getServerPageUrl("/admin/delivery")}>
                                    배송 관리
                                </a>
                            )}
                            {canManageAdmin && (
                                // 사용자 관리는 기존 관리자 화면을 그대로 열어 권한 관리 흐름을 유지함
                                <a className="home-secondary-button" href={getServerPageUrl("/admin/users")}>
                                    사용자 관리
                                </a>
                            )}
                        </div>

                        <dl className="home-account-meta">
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
                    </aside>
                ) : (
                    <aside className="home-account-panel">
                        <p className="home-panel-eyebrow">LOGIN</p>

                        <div className="home-account-actions">
                            <Link className="home-primary-button" to="/login">
                                로그인 페이지 이동
                            </Link>
                            <a className="home-secondary-button" href={getServerPageUrl("/signup")}>
                                회원가입
                            </a>
                            <a className="home-oauth-button naver" href={getOAuthLoginUrl("naver")}>
                                네이버 로그인
                            </a>
                            <a className="home-oauth-button google" href={getOAuthLoginUrl("google")}>
                                구글 로그인
                            </a>
                        </div>
                    </aside>
                )}
            </section>

            <section className="home-featured-section">
                <div className="home-section-head">
                    <div>
                        <p className="home-panel-eyebrow">PRODUCTS</p>
                        <h2>오늘의 추천 상품</h2>
                    </div>
                    <Link to="/products">전체 보기</Link>
                </div>

                {loadingProducts ? (
                    <p className="home-state-message">추천 상품을 불러오는 중입니다.</p>
                ) : productMessage ? (
                    <p className="home-state-message error">{productMessage}</p>
                ) : featuredProducts.length === 0 ? (
                    <p className="home-state-message">등록된 상품이 없습니다.</p>
                ) : (
                    <div className="home-product-grid">
                        {featuredProducts.map((product) => (
                            // 추천 상품 카드는 실제 상품 ID로 상세 화면에 연결함
                            <Link
                                className="home-product-card"
                                key={product.productId}
                                to={`/products/${product.productId}`}
                            >
                                <div className="home-product-image-box">
                                    {isImageUrl(product.mainImage) ? (
                                        <img
                                            className="home-product-image"
                                            src={product.mainImage}
                                            alt={product.productName}
                                        />
                                    ) : (
                                        <div className="home-product-placeholder">
                                            {product.category || "No Image"}
                                        </div>
                                    )}
                                </div>
                                <div className="home-product-body">
                                    <span className="home-product-badge">{product.category || "추천"}</span>
                                    <h3>{product.productName}</h3>
                                    <p>{product.sellerName || "판매자 정보 없음"}</p>
                                    <div className="home-product-bottom">
                                        <strong>{formatPrice(product.price)}</strong>
                                        <span className={isProductSoldOut(product) ? "sold-out" : ""}>
                                            {getStockLabel(product)}
                                        </span>
                                    </div>
                                </div>
                            </Link>
                        ))}
                    </div>
                )}
            </section>
        </section>
    );
}

// CloudFront 주소 끝의 슬래시를 정리해 이미지 경로 조합이 중복 슬래시를 만들지 않게 함
function normalizeHomeImageBaseUrl(baseUrl) {
    if (!baseUrl) {
        return "";
    }

    return baseUrl.replace(/\/+$/, "");
}

// 홈 배너 이미지 경로를 CloudFront 기본 주소와 조합하고 설정이 없으면 빈 값으로 fallback함
function getHomeImageUrl(imagePath) {
    if (!HOME_IMAGE_BASE_URL) {
        return "";
    }

    return `${HOME_IMAGE_BASE_URL}/${imagePath.replace(/^\/+/, "")}`;
}

// 인기 상품 API 응답을 화면에 바로 쓸 수 있는 상품 배열로 정리함
function normalizeProducts(result) {
    if (result.status !== "fulfilled") {
        return [];
    }

    return selectFeaturedProducts(result.value);
}

// 추천 상품 영역은 배열 여부를 확인한 뒤 최대 노출 개수만 남김
function selectFeaturedProducts(products) {
    if (!Array.isArray(products)) {
        return [];
    }

    return products.filter((product) => product?.productId).slice(0, FEATURED_PRODUCT_LIMIT);
}

// 홈 인사말은 이름, 아이디 순서로 안전하게 표시함
function getDisplayName(user) {
    return user?.userName || user?.loginId || "사용자";
}

// Spring Security 권한 표기 차이를 흡수해 SELLER와 ROLE_SELLER를 같이 인정함
function hasRole(user, roleName) {
    const role = String(user?.role ?? "");
    return role === roleName || role === `ROLE_${roleName}`;
}

// 상품 품절 판단을 카드 배지와 문구에서 같이 사용함
function isProductSoldOut(product) {
    return product.isSoldOut || product.quantity === 0;
}

// 상품 재고 문구를 품절, 판매량, 재고 순서로 간단히 표시함
function getStockLabel(product) {
    if (isProductSoldOut(product)) {
        return "품절";
    }

    if (product.salesCount > 0) {
        return `판매 ${product.salesCount}회`;
    }

    return `재고 ${product.quantity ?? 0}개`;
}

// provider 원본값을 사용자가 읽기 쉬운 회원 유형으로 바꿈
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

export default HomePage;
