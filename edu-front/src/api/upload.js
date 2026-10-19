import request from '@/utils/request'

// 上传文件到阿里云 OSS，返回文件的公网访问地址。
//
// ⚠️ 后端是从**系统环境变量** OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET 读密钥的。
// 没配这两个变量的话，点上传会报错（后端那边连不上 OSS）。
// 配好之后要**重启后端**才能生效，因为密钥是在服务启动时读一次。
export function uploadFile(file) {
  const formData = new FormData()
  // 字段名必须叫 file，和后端 upload(MultipartFile file) 的参数名一致
  formData.append('file', file)
  return request.post('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
