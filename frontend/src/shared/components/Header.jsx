import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router";
// 공통 헤더에서도 로그인 상태를 보여주기 위해 사용자 API를 확인함
import { getCurrentUser, logoutUser } from "../../pages/auth/authApi";

function Header() {
  // 현재 로그인 사용자를 헤더 링크 전환에 사용함
  const [currentUser, setCurrentUser] = useState(null);
  // 라우트가 바뀐 뒤 로그인 상태를 다시 확인해 로그인 직후 헤더를 갱신함
  const location = useLocation();
  // 로그아웃 후 로그인 화면으로 이동시키기 위해 navigate를 사용함
  const navigate = useNavigate();

  useEffect(() => {
    let ignore = false;

    async function fetchCurrentUser() {
      try {
        const user = await getCurrentUser();
        if (!ignore) {
          setCurrentUser(user);
        }
      } catch {
        if (!ignore) {
          setCurrentUser(null);
        }
      }
    }

    // 페이지 이동마다 세션 상태를 다시 읽어 헤더의 로그인 표시를 맞춤
    fetchCurrentUser();

    return () => {
      ignore = true;
    };
  }, [location.pathname]);

  // 로그아웃 API 호출 후 헤더 상태를 비우고 로그인 화면으로 보냄
  async function handleLogout() {
    try {
      await logoutUser();
    } finally {
      setCurrentUser(null);
      navigate("/login");
    }
  }

  // 헤더 검색은 상품 목록 query로 이동해 기존 검색 API 흐름을 재사용함
  function handleHeaderSearchSubmit(event) {
    event.preventDefault();

    const formData = new FormData(event.currentTarget);
    const keyword = formData.get("keyword")?.toString().trim() ?? "";
    const category = formData.get("category")?.toString() ?? "";
    const params = new URLSearchParams();

    if (category) {
      params.set("category", category);
    }
    if (keyword) {
      params.set("keyword", keyword);
    }

    navigate(params.toString() ? `/products?${params.toString()}` : "/products");
  }

  return (
    // Thymeleaf 쇼핑 화면의 상단 구조와 톤을 맞추기 위해 공통 헤더를 2단으로 정리함
    <header className="site-header">
      <div className="utility-bar">
        <div className="site-container utility-inner">
          <span>오늘 도착처럼 빠르게, 장인 제품도 간편하게.</span>
          <nav className="utility-links" aria-label="계정 메뉴">
            {currentUser ? (
              <>
                <Link to="/login">{currentUser.userName || currentUser.loginId}님</Link>
                <button className="utility-button" type="button" onClick={handleLogout}>
                  로그아웃
                </button>
              </>
            ) : (
              <Link to="/login">로그인</Link>
            )}
            <Link to="/cart">장바구니</Link>
          </nav>
        </div>
      </div>

      <div className="main-header">
        <div className="site-container main-header-inner">
          <h1 className="logo">
            <Link to="/">
              HandMade <em>Market</em>
            </Link>
          </h1>

          {/* 공통 헤더에서 바로 상품 검색으로 이동하게 해 홈/목록 진입 검색을 통일함 */}
          <form className="header-search-form" onSubmit={handleHeaderSearchSubmit}>
            <label className="header-search-label" htmlFor="header-category">
              카테고리
            </label>
            <select id="header-category" name="category">
              <option value="">전체</option>
              <option value="비누">비누</option>
              <option value="목공">목공</option>
              <option value="패브릭">패브릭</option>
              <option value="문구">문구</option>
            </select>

            <label className="header-search-label" htmlFor="header-keyword">
              검색어
            </label>
            <input
              id="header-keyword"
              name="keyword"
              type="search"
              placeholder="찾고 싶은 핸드메이드 제품을 검색해보세요"
            />

            <button className="header-search-button" type="submit">
              검색
            </button>
          </form>

          <nav className="nav" aria-label="주요 메뉴">
            <Link to="/">홈</Link>
            <Link to="/products">상품 둘러보기</Link>
            <Link to="/cart">장바구니</Link>
            <Link to="/orders">주문 내역</Link>
          </nav>
        </div>
      </div>
    </header>
  );
}

export default Header;
