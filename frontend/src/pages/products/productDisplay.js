// 상품 설명 필드가 이미지 URL로도 쓰일 수 있어 화면 분기 기준을 한 곳에서 관리함
export function isImageUrl(value) {
    return /^(https?:)?\/\//i.test(value) || /\.(png|jpg|jpeg|gif|webp|svg)(\?.*)?$/i.test(value);
}

// 상품 가격 표기를 한국어 숫자 형식으로 통일함
export function formatPrice(value) {
    return `${Number(value ?? 0).toLocaleString("ko-KR")}원`;
}

// 현재 DTO의 mainImage 값이 설명 문자열인 경우 상세 설명으로 사용함
export function getProductDescription(product) {
    const mainImage = product?.mainImage?.trim();

    if (!mainImage || isImageUrl(mainImage)) {
        return "판매자가 등록한 상세 설명이 아직 없습니다.";
    }

    return mainImage;
}
