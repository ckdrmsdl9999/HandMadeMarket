import { Link } from "react-router";

function Header() {
  return (
    // Thymeleaf 쇼핑 화면의 상단 구조와 톤을 맞추기 위해 공통 헤더를 2단으로 정리함
    <header className="site-header">
      <div className="utility-bar">
        <div className="site-container utility-inner">
          <span>오늘 도착처럼 빠르게, 장인 제품도 간편하게.</span>
          <nav className="utility-links" aria-label="계정 메뉴">
            <Link to="/login">로그인</Link>
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
