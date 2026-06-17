import { Link } from "react-router";

function Header() {
  return (
    <header className="header">
      <h1 className="logo">
        <Link to="/">HandMadeMarket</Link>
      </h1>

      <nav className="nav">
        <Link to="/">홈</Link>
        <Link to="/products">상품</Link>
        <Link to="/cart">장바구니</Link>
        <Link to="/orders">주문</Link>
        <Link to="/login">로그인</Link>
      </nav>
    </header>
  );
}

export default Header;
