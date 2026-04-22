# Browser Test Prompts

This file contains copy-paste prompts for testing the downloader flow in a real browser.

The prompts below are written for a browser agent or AI QA operator that can open pages, click UI controls, wait for state changes, and report observed results.

## Preconditions

- Backend should be running at `http://localhost:8080`
- Preferred frontend route is `http://localhost:5173/app/downloader`
- If the Vite frontend is not running, use the backend-rendered fallback at `http://localhost:8080/downloader`
- Default accounts seeded by the backend:
  - `user@test.com` / `user`
  - `pub@test.com` / `pub`
  - `admin@test.com` / `admin`

Notes:

- The React prompts below target `/app/downloader`
- Login still goes through Spring Security at `/login`
- `user@test.com` has a daily quota of 3 jobs
- `pub@test.com` has a daily quota of 20 jobs

## Prompt 1: Quick Smoke Test For Direct URL

Use this prompt when you want to validate the main business flow from login to completed job and downloadable files.

```text
Bạn là QA browser agent. Hãy test luồng downloader end-to-end bằng UI thật, không đoán kết quả và không sửa code.

Mục tiêu:
- Đăng nhập thành công
- Submit 1 source request từ URL public
- Xác nhận source request và job được tạo
- Theo dõi job cho tới khi vào trạng thái kết thúc
- Nếu completed thì xác nhận có file đầu ra
- Nếu failed hoặc blocked thì ghi lại lỗi đúng như UI hiển thị

Môi trường:
- Frontend ưu tiên: http://localhost:5173/app/downloader
- Nếu frontend dev server không chạy thì dùng fallback: http://localhost:8080/downloader
- Login page: /login
- Tài khoản mặc định ưu tiên: user@test.com / user
- Nếu bị hết quota trong ngày, chuyển sang pub@test.com / pub và ghi rõ việc chuyển tài khoản trong báo cáo
- URL test: https://www.youtube.com/watch?v=dQw4w9WgXcQ

Yêu cầu thực hiện:
1. Mở route downloader workspace.
2. Nếu chưa đăng nhập, đi tới /login và đăng nhập bằng tài khoản đã nêu.
3. Sau khi đăng nhập, quay lại downloader workspace nếu hệ thống không tự quay lại đó.
4. Xác nhận UI đã nhận diện session thành công, ví dụ thấy email hiện tại hoặc khu vực session/quota.
5. Trong form "Submit Source Request", điền:
   - URL nguồn: https://www.youtube.com/watch?v=dQw4w9WgXcQ
   - Platform: giữ "Tự nhận diện"
   - Source type: giữ "Tự suy luận"
   - Loại tải: "Video"
   - Format: "MP4"
   - Chất lượng ưu tiên: "Tốt nhất"
   - Giữ mặc định các tuỳ chọn còn lại
6. Bấm nút "Tạo source request".
7. Xác nhận source request mới xuất hiện trong UI.
8. Xác nhận job mới xuất hiện trong danh sách job.
9. Mở chi tiết job nếu UI chưa tự chọn job đó.
10. Theo dõi trong tối đa 3 phút:
    - trạng thái job
    - progress %
    - download speed
    - ETA
    - log có xuất hiện hay không
11. Nếu job completed:
    - xác nhận panel "File tải xuống" xuất hiện
    - xác nhận có ít nhất 1 file
    - xác nhận có nút "Tải file" cho ít nhất 1 file
12. Nếu job failed hoặc blocked:
    - ghi lại nguyên văn thông báo lỗi đang hiển thị
    - nếu có log liên quan thì trích các dòng quan trọng nhất
13. Không suy diễn. Nếu bị chặn bởi môi trường như backend chưa chạy, quota hết, mạng ngoài lỗi, yt-dlp lỗi, hoặc UI không load được, hãy dừng và ghi đúng lý do.

Đầu ra cuối cùng phải có:
- URL đã mở
- Có đăng nhập được hay không
- Tài khoản đã dùng
- Source request id nếu nhìn thấy được
- Job id nếu nhìn thấy được
- Trạng thái cuối cùng của job
- Có thấy progress/log/file hay không
- Nếu có file thì số lượng file
- Nếu lỗi thì thông báo lỗi chính xác
- Nhận định ngắn: PASS / FAIL / BLOCKED
```

## Prompt 2: Fan-Out Test For Playlist Or Profile Flow

Use this prompt when you want to validate the branch where one `SourceRequest` resolves into multiple jobs.

Replace `<PLAYLIST_OR_PROFILE_URL>` before running.

```text
Bạn là QA browser agent. Hãy test nhánh playlist/profile của downloader bằng UI thật, không đoán và không sửa code.

Mục tiêu:
- Đăng nhập bằng tài khoản có quota rộng hơn
- Submit 1 source request dạng playlist hoặc profile
- Xác nhận source request được resolve thành nhiều job
- Xác nhận hệ thống poll và hiển thị được nhiều job con

Môi trường:
- Frontend ưu tiên: http://localhost:5173/app/downloader
- Nếu frontend dev server không chạy thì dùng fallback: http://localhost:8080/downloader
- Login page: /login
- Tài khoản ưu tiên: pub@test.com / pub
- URL test: <PLAYLIST_OR_PROFILE_URL>

Yêu cầu thực hiện:
1. Mở downloader workspace.
2. Đăng nhập bằng pub@test.com / pub nếu chưa có session.
3. Trong form "Submit Source Request", điền:
   - URL nguồn: <PLAYLIST_OR_PROFILE_URL>
   - Platform: chọn đúng nền tảng nếu biết rõ, nếu không thì giữ "Tự nhận diện"
   - Source type: ưu tiên chọn "Playlist" hoặc "Profile / Channel" cho đúng loại URL, nếu không chắc mới giữ "Tự suy luận"
   - Loại tải: "Video"
   - Format: "MP4"
   - Chất lượng ưu tiên: "Tốt nhất"
4. Bấm "Tạo source request".
5. Theo dõi danh sách source request và job trong tối đa 3 phút.
6. Xác nhận một trong các kết quả sau:
   - source request đi qua `RESOLVING` rồi `RESOLVED`
   - hoặc source request bị `BLOCKED` với lý do rõ ràng
7. Nếu flow thành công, xác nhận có nhiều hơn 1 job được tạo từ cùng source request.
8. Chọn ít nhất 1 job con để kiểm tra chi tiết và log.
9. Nếu không tạo được nhiều job, ghi rõ hệ thống chỉ tạo được bao nhiêu job và trạng thái source request cuối cùng.
10. Không suy diễn. Nếu playlist/profile public không truy cập được hoặc provider chặn, ghi đúng lý do.

Đầu ra cuối cùng phải có:
- URL đã test
- Loại flow đã test: playlist hay profile
- Source request state cuối cùng
- Số job được tạo
- Có ít nhất 1 job chạy được hay không
- Trạng thái cuối cùng của các job nhìn thấy được
- Nhận định ngắn: PASS / FAIL / BLOCKED
```

## Prompt 3: Negative Test For Proxy Policy

Use this prompt when you want to validate one of the access policy checks for a normal user.

```text
Bạn là QA browser agent. Hãy test rule chặn proxy đối với tài khoản USER bằng UI thật, không đoán và không sửa code.

Mục tiêu:
- Đăng nhập bằng user@test.com / user
- Submit request có trường Proxy
- Xác nhận backend từ chối vì USER không được dùng proxy riêng

Môi trường:
- Frontend ưu tiên: http://localhost:5173/app/downloader
- Nếu frontend dev server không chạy thì dùng fallback: http://localhost:8080/downloader
- Login page: /login
- Tài khoản: user@test.com / user
- URL test: https://www.youtube.com/watch?v=dQw4w9WgXcQ
- Proxy test value: http://127.0.0.1:8080

Yêu cầu thực hiện:
1. Mở downloader workspace và đăng nhập bằng user@test.com / user.
2. Trong form submit:
   - URL nguồn: https://www.youtube.com/watch?v=dQw4w9WgXcQ
   - Giữ các field cơ bản ở chế độ mặc định
3. Mở phần "Tùy chọn nâng cao".
4. Điền trường "Proxy" bằng `http://127.0.0.1:8080`.
5. Bấm "Tạo source request".
6. Xác nhận request không được chấp nhận.
7. Ghi lại nguyên văn thông báo lỗi hiển thị trên UI.
8. Không tiếp tục workaround bằng tài khoản khác trong cùng ca test này.

Đầu ra cuối cùng phải có:
- Có submit được hay không
- Lỗi hiển thị chính xác
- Nhận định rule proxy cho USER đang hoạt động hay không
```

## Suggested Use

- Use Prompt 1 for a quick smoke test
- Use Prompt 2 when validating the resolve and fan-out branch
- Use Prompt 3 when validating access policy behavior

If needed, these prompts can later be converted into Playwright E2E test cases.
