import { useCallback, useEffect, useState } from 'react'
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  CloudUploadOutlined,
} from '@ant-design/icons'
import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  createInterviewJobPosition,
  createInterviewQuestion,
  deleteInterviewJobPosition,
  deleteInterviewQuestion,
  getInterviewJobPositions,
  getInterviewQuestions,
  syncInterviewQuestionToChroma,
  updateInterviewJobPosition,
  updateInterviewQuestion,
} from '../../api/interview'

const { Title, Text } = Typography
const { TextArea } = Input

const SUITABLE_LEVELS = ['初级', '中级', '高级']

function InterviewManage() {
  const [activeTab, setActiveTab] = useState('questions')

  // ---- questions ----
  const [qLoading, setQLoading] = useState(false)
  const [qList, setQList] = useState([])
  const [qTotal, setQTotal] = useState(0)
  const [qPage, setQPage] = useState(1)
  const [qPageSize, setQPageSize] = useState(20)
  const [qKeyword, setQKeyword] = useState('')
  const [qJob, setQJob] = useState()
  const [qType, setQType] = useState()
  const [qModalOpen, setQModalOpen] = useState(false)
  const [qEditing, setQEditing] = useState(null)
  const [qSubmitting, setQSubmitting] = useState(false)
  const [qForm] = Form.useForm()

  // ---- jobs ----
  const [jLoading, setJLoading] = useState(false)
  const [jList, setJList] = useState([])
  const [jModalOpen, setJModalOpen] = useState(false)
  const [jEditing, setJEditing] = useState(null)
  const [jSubmitting, setJSubmitting] = useState(false)
  const [jForm] = Form.useForm()

  const loadQuestions = useCallback(async () => {
    setQLoading(true)
    try {
      const res = await getInterviewQuestions({
        page: qPage,
        pageSize: qPageSize,
        q: qKeyword || undefined,
        jobPosition: qJob || undefined,
        questionType: qType || undefined,
      })
      const data = res?.data || res || {}
      setQList(data.items || [])
      setQTotal(data.total || 0)
    } catch (e) {
      message.error(e?.message || '加载题库失败')
    } finally {
      setQLoading(false)
    }
  }, [qPage, qPageSize, qKeyword, qJob, qType])

  const loadJobs = useCallback(async () => {
    setJLoading(true)
    try {
      const res = await getInterviewJobPositions()
      const data = res?.data || res || {}
      setJList(data.items || [])
    } catch (e) {
      message.error(e?.message || '加载岗位失败')
    } finally {
      setJLoading(false)
    }
  }, [])

  useEffect(() => {
    if (activeTab === 'questions') loadQuestions()
  }, [activeTab, loadQuestions])

  useEffect(() => {
    if (activeTab === 'jobs') loadJobs()
  }, [activeTab, loadJobs])

  const openCreateQuestion = () => {
    setQEditing(null)
    qForm.resetFields()
    qForm.setFieldsValue({
      difficulty: 3,
      status: 1,
      suitable_level: '中级',
    })
    setQModalOpen(true)
  }

  const openEditQuestion = (record) => {
    setQEditing(record)
    qForm.setFieldsValue({
      job_position: record.job_position,
      question_type: record.question_type,
      question: record.question,
      excellent_answer: record.excellent_answer,
      difficulty: record.difficulty ?? 3,
      keywords: record.keywords,
      question_intent: record.question_intent,
      answer_points: record.answer_points,
      score_standard: record.score_standard,
      suitable_level: record.suitable_level || '中级',
      remark: record.remark,
      status: record.status ?? 1,
    })
    setQModalOpen(true)
  }

  const submitQuestion = async () => {
    try {
      const values = await qForm.validateFields()
      setQSubmitting(true)
      if (qEditing?.id) {
        await updateInterviewQuestion(qEditing.id, values)
        message.success('已更新题目')
      } else {
        await createInterviewQuestion(values)
        message.success('已创建题目')
      }
      setQModalOpen(false)
      loadQuestions()
    } catch (e) {
      if (e?.errorFields) return
      message.error(e?.message || '保存失败')
    } finally {
      setQSubmitting(false)
    }
  }

  const removeQuestion = async (id) => {
    try {
      await deleteInterviewQuestion(id)
      message.success('已删除')
      loadQuestions()
    } catch (e) {
      message.error(e?.message || '删除失败')
    }
  }

  const syncChroma = async (id) => {
    try {
      await syncInterviewQuestionToChroma(id)
      message.success('已同步向量库')
      loadQuestions()
    } catch (e) {
      message.error(e?.message || '同步失败（可检查 Chroma 配置）')
    }
  }

  const openCreateJob = () => {
    setJEditing(null)
    jForm.resetFields()
    jForm.setFieldsValue({ status: 1 })
    setJModalOpen(true)
  }

  const openEditJob = (record) => {
    setJEditing(record)
    jForm.setFieldsValue({
      job_position: record.job_position,
      tech_stack: record.tech_stack,
      status: record.status ?? 1,
      remark: record.remark,
    })
    setJModalOpen(true)
  }

  const submitJob = async () => {
    try {
      const values = await jForm.validateFields()
      setJSubmitting(true)
      if (jEditing?.id) {
        await updateInterviewJobPosition(jEditing.id, values)
        message.success('已更新岗位')
      } else {
        await createInterviewJobPosition(values)
        message.success('已创建岗位')
      }
      setJModalOpen(false)
      loadJobs()
    } catch (e) {
      if (e?.errorFields) return
      message.error(e?.message || '保存失败')
    } finally {
      setJSubmitting(false)
    }
  }

  const removeJob = async (id) => {
    try {
      await deleteInterviewJobPosition(id)
      message.success('已删除')
      loadJobs()
    } catch (e) {
      message.error(e?.message || '删除失败')
    }
  }

  const questionColumns = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '岗位', dataIndex: 'job_position', width: 120, ellipsis: true },
    { title: '题型', dataIndex: 'question_type', width: 100, ellipsis: true },
    {
      title: '题目',
      dataIndex: 'question',
      ellipsis: true,
      render: (v) => <Text style={{ maxWidth: 320 }} ellipsis={{ tooltip: v }}>{v}</Text>,
    },
    {
      title: '难度',
      dataIndex: 'difficulty',
      width: 70,
      render: (v) => <Tag>{v ?? '-'}</Tag>,
    },
    {
      title: '层级',
      dataIndex: 'suitable_level',
      width: 80,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (v) => (v === 1 ? <Tag color="green">启用</Tag> : <Tag>停用</Tag>),
    },
    {
      title: '向量库',
      dataIndex: 'in_chroma',
      width: 80,
      render: (v) => (v === 1 ? <Tag color="blue">已入</Tag> : <Tag>未入</Tag>),
    },
    {
      title: '操作',
      width: 220,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEditQuestion(record)}>
            编辑
          </Button>
          <Button type="link" size="small" icon={<CloudUploadOutlined />} onClick={() => syncChroma(record.id)}>
            同步
          </Button>
          <Popconfirm title="确认删除该题目？" onConfirm={() => removeQuestion(record.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const jobColumns = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '岗位', dataIndex: 'job_position', width: 160 },
    {
      title: '技术栈',
      dataIndex: 'tech_stack',
      ellipsis: true,
      render: (v) => <Text ellipsis={{ tooltip: v }}>{v}</Text>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (v) => (v === 1 ? <Tag color="green">启用</Tag> : <Tag>停用</Tag>),
    },
    { title: '备注', dataIndex: 'remark', ellipsis: true },
    {
      title: '操作',
      width: 160,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEditJob(record)}>
            编辑
          </Button>
          <Popconfirm title="确认删除该岗位？" onConfirm={() => removeJob(record.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <Title level={4} style={{ marginBottom: 4 }}>
        AI 面试配置
      </Title>
      <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
        管理面试题库与岗位技术栈，供前端「折跃同步」模拟面试与题库练习使用。
      </Text>

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: 'questions',
            label: '题库管理',
            children: (
              <>
                <Space wrap style={{ marginBottom: 16 }}>
                  <Input.Search
                    allowClear
                    placeholder="搜索题目"
                    style={{ width: 220 }}
                    onSearch={(v) => {
                      setQPage(1)
                      setQKeyword(v)
                    }}
                  />
                  <Input
                    allowClear
                    placeholder="岗位筛选"
                    style={{ width: 160 }}
                    onChange={(e) => setQJob(e.target.value || undefined)}
                    onBlur={() => {
                      setQPage(1)
                      loadQuestions()
                    }}
                  />
                  <Input
                    allowClear
                    placeholder="题型筛选"
                    style={{ width: 140 }}
                    onChange={(e) => setQType(e.target.value || undefined)}
                    onBlur={() => {
                      setQPage(1)
                      loadQuestions()
                    }}
                  />
                  <Button icon={<ReloadOutlined />} onClick={loadQuestions}>
                    刷新
                  </Button>
                  <Button type="primary" icon={<PlusOutlined />} onClick={openCreateQuestion}>
                    新建题目
                  </Button>
                </Space>
                <Table
                  rowKey="id"
                  loading={qLoading}
                  columns={questionColumns}
                  dataSource={qList}
                  scroll={{ x: 1100 }}
                  pagination={{
                    current: qPage,
                    pageSize: qPageSize,
                    total: qTotal,
                    showSizeChanger: true,
                    onChange: (p, ps) => {
                      setQPage(p)
                      setQPageSize(ps)
                    },
                  }}
                />
              </>
            ),
          },
          {
            key: 'jobs',
            label: '岗位技术栈',
            children: (
              <>
                <Space style={{ marginBottom: 16 }}>
                  <Button icon={<ReloadOutlined />} onClick={loadJobs}>
                    刷新
                  </Button>
                  <Button type="primary" icon={<PlusOutlined />} onClick={openCreateJob}>
                    新建岗位
                  </Button>
                </Space>
                <Table
                  rowKey="id"
                  loading={jLoading}
                  columns={jobColumns}
                  dataSource={jList}
                  pagination={false}
                />
              </>
            ),
          },
        ]}
      />

      <Modal
        title={qEditing ? '编辑面试题' : '新建面试题'}
        open={qModalOpen}
        onCancel={() => setQModalOpen(false)}
        onOk={submitQuestion}
        confirmLoading={qSubmitting}
        width={720}
        destroyOnClose
      >
        <Form form={qForm} layout="vertical">
          <Space style={{ width: '100%' }} size="middle">
            <Form.Item
              name="job_position"
              label="岗位"
              rules={[{ required: true, message: '请输入岗位' }]}
              style={{ width: 240 }}
            >
              <Input placeholder="如：Java 后端" />
            </Form.Item>
            <Form.Item
              name="question_type"
              label="题型"
              rules={[{ required: true, message: '请输入题型' }]}
              style={{ width: 200 }}
            >
              <Input placeholder="如：技术面 / 行为面" />
            </Form.Item>
            <Form.Item name="difficulty" label="难度 1-5" style={{ width: 120 }}>
              <InputNumber min={1} max={5} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Form.Item
            name="question"
            label="题目内容"
            rules={[{ required: true, message: '请输入题目' }]}
          >
            <TextArea rows={4} />
          </Form.Item>
          <Form.Item name="excellent_answer" label="优秀答案示例">
            <TextArea rows={3} />
          </Form.Item>
          <Form.Item name="answer_points" label="答题要点">
            <TextArea rows={2} />
          </Form.Item>
          <Form.Item name="score_standard" label="评分标准">
            <TextArea rows={2} />
          </Form.Item>
          <Space style={{ width: '100%' }} size="middle">
            <Form.Item name="keywords" label="关键词" style={{ width: 280 }}>
              <Input placeholder="逗号分隔" />
            </Form.Item>
            <Form.Item name="question_intent" label="考察意图" style={{ width: 280 }}>
              <Input />
            </Form.Item>
            <Form.Item name="suitable_level" label="适用层级" style={{ width: 120 }}>
              <Select options={SUITABLE_LEVELS.map((v) => ({ value: v, label: v }))} />
            </Form.Item>
          </Space>
          <Space>
            <Form.Item name="status" label="状态" style={{ width: 120 }}>
              <Select
                options={[
                  { value: 1, label: '启用' },
                  { value: 0, label: '停用' },
                ]}
              />
            </Form.Item>
            <Form.Item name="remark" label="备注" style={{ width: 400 }}>
              <Input />
            </Form.Item>
          </Space>
        </Form>
      </Modal>

      <Modal
        title={jEditing ? '编辑岗位' : '新建岗位'}
        open={jModalOpen}
        onCancel={() => setJModalOpen(false)}
        onOk={submitJob}
        confirmLoading={jSubmitting}
        destroyOnClose
      >
        <Form form={jForm} layout="vertical">
          <Form.Item
            name="job_position"
            label="岗位名称"
            rules={[{ required: true, message: '请输入岗位' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="tech_stack" label="技术栈">
            <TextArea rows={4} placeholder="如：Java, Spring Boot, MySQL, Redis" />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              options={[
                { value: 1, label: '启用' },
                { value: 0, label: '停用' },
              ]}
            />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default InterviewManage
