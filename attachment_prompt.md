원래 다른 팀원 담당이었던 첨부파일 다운로드 기능도 내가 이어서 구현하려고 해.
아까 찾아낸 InboxService.java의 // TODO: Gmail API를 통한 실제 첨부파일 다운로드 주석 부분을 실제 구글 API 연동 코드로 완성해 줘.

[API 정보 및 상세 명세]

Target API: GET /api/inbox/{email_id}/attachments/{attachment_id}

Parameter
속성
전송방향
Type
Description
IN
OUT
Authorization
O

String
요청한 사용자 인증 토큰
email_id
O

int
조회할 이메일 고유 ID
attachment_id
O

int
다운로드할 첨부파일 ID
Content-Type


String
첨부파일의 원본 MIME 타입 (예: application/pdf) (Header)
Content-Disposition


String
attachment; filename="원본_파일명.확장자" (Header)
(File Body)


Binary
파일 원본 바이트 스트림 (Body)
(Error Only)  content_type

O
String
에러 발생 시 공통 규약에 따라 application/json 반환
(Error Only) success

O
Boolean
에러 발생 시 false
(Error Only) result_code


int
처리결과 코드 (200: 성공, 4xx/5xx: 오류코드)
(Error Only) result_req


String
처리결과 오류 메시지


실제 전송내용 (JSON 의 경우라면)


전송방향 : IN
// Headers
{
"Authorization" : "Bearer {accessToken}"
}

전송방향 : OUT (성공 시 - 파일 다운로드)
// Headers
HTTP/1.1 200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="회사소개서_및_견적서.pdf"
Content-Length: 2048576

// Body
(바이너리 파일 스트림 데이터 - JSON이 아님)

전송방향 : OUT (실패 시 - 공통 응답 규칙 적용)
// Headers
// Content-Type: application/json

// Body
{
"content_type" : "application/json",
"success" : false,
"result_code" : 404,
"result_req" : "ERR_ATTACHMENT_NOT_FOUND: 해당 이메일에서 요청하신 첨부파일을 찾을 수 없습니다."
}



[API 정보 및 상세 명세]

Method & URI: GET /api/inbox/{email_id}/attachments/{attachment_id}

성공 응답 (예외 적용): JSON이 아닌 바이너리 파일 스트림(ResponseEntity<byte[]>) 반환

Header: Content-Type (원본 MIME 타입), Content-Disposition (attachment; filename="파일명.확장자", 한글 깨짐 방지 URLEncoder 적용)

Body: 파일 원본 바이트 배열

실패 응답: 에러 발생 시 파일 스트림이 아닌, 기존 claude.md 규칙에 따른 공통 JSON 규약(BaseResponse) 반환

[구현 시 핵심 주의사항]

응답 형태 분리 구현: >    성공 시에는 Controller에서 ResponseEntity.ok().headers(...).body(byteData) 형태로 반환해 줘.
에러 발생 시에는 직접 JSON을 만들지 말고 적절한 커스텀 예외(예: AttachmentNotFoundException, GmailApiCallException 등)를 throw해서, 이미 존재하는 @RestControllerAdvice(GlobalExceptionHandler)가 이를 캐치해 BaseResponse 형식의 에러 JSON을 내보내도록 해줘.

GmailApiService 확장: >    GmailApiService에 첨부파일 데이터를 가져오는 메서드를 추가해 줘. userId, emailId(Google의 messageId), attachmentId(Google의 attachmentId)를 넘겨서 gmail.users().messages().attachments().get()을 호출해야 해.

Base64URL 디코딩 (필수): >    Gmail API가 반환하는 첨부파일 데이터는 Base64URL 형식이야. 이를 반드시 Base64.getUrlDecoder().decode() 등을 사용하여 순수 byte[]로 디코딩한 후 반환해야 해.

DB 메타데이터 활용: >    파일의 원본 이름(filename)과 MIME 타입은 우리 DB의 첨부파일 엔티티에 저장되어 있을 테니, 이를 먼저 조회해서 HTTP 응답 헤더 세팅에 사용해 줘.

claude.md의 응답 규칙에 대한 유일한 예외 API임을 인지하고 코드를 작성해 줘.

[첨부파일 처리 프로세스 요약]
수집 시점 (팀원 1 담당): 메일을 처음 긁어올 때 파일 덩어리(Binary)는 버리고, 이름, 사이즈, 구글 전용 attachmentId만 추출해서 DB에 저장한다.

화면 표시: 사용자가 메일을 볼 때 DB에 저장된 파일 이름 목록만 먼저 보여준다.

클릭 시점 (지금 구현할 것): 사용자가 클릭하면 백엔드가 DB에서 attachmentId를 꺼내 Gmail API(attachments.get)를 호출한다.

반환: 구글이 준 데이터를 Base64URL 디코딩하여 순수 바이트(byte[])로 변환 후, 명세서에 적힌 대로 파일 스트림으로 쏴준다.