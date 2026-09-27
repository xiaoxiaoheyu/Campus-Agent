import request from '../utils/request'

const questionsBase = '/api/admin/interview/questions'
const jobsBase = '/api/admin/interview/job-positions'

/** 面试题库列表 */
export const getInterviewQuestions = (params) =>
  request.get(questionsBase, { params })

/** 面试题目详情 */
export const getInterviewQuestionDetail = (id) =>
  request.get(`${questionsBase}/${id}`)

/** 新建面试题 */
export const createInterviewQuestion = (data) =>
  request.post(questionsBase, data)

/** 更新面试题 */
export const updateInterviewQuestion = (id, data) =>
  request.put(`${questionsBase}/${id}`, data)

/** 删除面试题 */
export const deleteInterviewQuestion = (id) =>
  request.delete(`${questionsBase}/${id}`)

/** 岗位技术栈列表 */
export const getInterviewJobPositions = () =>
  request.get(jobsBase)

/** 新建岗位 */
export const createInterviewJobPosition = (data) =>
  request.post(jobsBase, data)

/** 更新岗位 */
export const updateInterviewJobPosition = (id, data) =>
  request.put(`${jobsBase}/${id}`, data)

/** 删除岗位 */
export const deleteInterviewJobPosition = (id) =>
  request.delete(`${jobsBase}/${id}`)

/** 同步题目到向量库（Chroma） */
export const syncInterviewQuestionToChroma = (id) =>
  request.post('/api/chroma/create', { id })
