<template>
	<view class="ai-assistant-root">
		<view
			v-if="panelVisible"
			class="ai-assistant-mask"
			@click="closePanel"
		></view>

		<view
			v-if="panelVisible"
			class="ai-assistant-panel"
			:class="{ 'ai-assistant-panel--active': panelVisible }"
			:style="panelStyle"
		>
			<view class="ai-assistant-panel__header">
				<view>
					<text class="ai-assistant-panel__eyebrow">Campus Copilot</text>
					<text class="ai-assistant-panel__title">校园对话助手</text>
				</view>
				<view class="ai-assistant-panel__actions">
					<view class="ai-assistant-panel__icon" @click.stop="openFullConversation">展开</view>
					<view class="ai-assistant-panel__icon" @click="resetSession">新会话</view>
					<view class="ai-assistant-panel__close" @click="closePanel">×</view>
				</view>
			</view>

			<scroll-view
				class="ai-assistant-panel__messages"
				scroll-y
				enhanced
				show-scrollbar="false"
				:scroll-into-view="scrollAnchor"
			>
				<view
					v-for="(message, index) in messages"
					:key="message.localId || message.id"
					:id="`ai-message-${index}`"
					class="ai-message-row"
					:class="message.role === 'user' ? 'ai-message-row--user' : 'ai-message-row--assistant'"
				>
					<view class="ai-message-bubble" :class="message.role === 'user' ? 'ai-message-bubble--user' : 'ai-message-bubble--assistant'">
						<view v-if="message.type === 'thinking'" class="ai-thinking-indicator">
							<text class="ai-thinking-text">思考中</text>
							<view class="ai-thinking-dots">
								<text></text>
								<text></text>
								<text></text>
							</view>
						</view>
						<view v-else-if="message.role === 'assistant'" class="ai-message-content">
							<view v-if="getOutputTypeTags(message).length" class="ai-output-type-list">
								<text
									v-for="type in getOutputTypeTags(message)"
									:key="`${message.localId || message.id}-type-${type}`"
									class="ai-output-type-tag"
								>{{ getOutputTypeLabel(type) }}</text>
							</view>
							<safe-markdown
								v-if="getDisplayContent(message)"
								class="ai-message-markdown"
								:content="getDisplayContent(message)"
							/>

							<view v-if="getMessageResources(message).length" class="ai-resource-list">
								<view
									v-for="resource in getMessageResources(message)"
									:key="`${message.localId || message.id}-${resource.key}`"
									class="ai-resource-card"
									:class="[`ai-resource-card--${resource.renderer}`, { 'ai-resource-card--unavailable': resource.unavailable }]"
								>
									<view
										v-if="isFileResource(resource)"
										class="ai-resource-file"
										:class="{ 'ai-resource-file--clickable': getPrimaryFileAction(resource) }"
										@click="openFileResource(resource, message)"
									>
										<view class="ai-resource-file__icon" :class="`ai-resource-file__icon--${getResourceFileClass(resource)}`">
											<view class="ai-resource-file__fold"></view>
											<text class="ai-resource-file__extension">{{ getResourceFileExtension(resource) }}</text>
										</view>
										<view class="ai-resource-file__body">
											<text class="ai-resource-file__name">{{ resource.title }}</text>
											<text class="ai-resource-file__meta">{{ getResourceSizeLabel(resource) }}{{ getPrimaryFileAction(resource) ? ' · 点击下载' : '' }}</text>
										</view>
									</view>
									<view v-else class="ai-resource-card__header">
										<text class="ai-resource-card__icon">{{ getResourceIcon(resource) }}</text>
										<view class="ai-resource-card__heading">
											<text class="ai-resource-card__title">{{ resource.title }}</text>
											<text class="ai-resource-card__meta">
												{{ getResourceKindLabel(resource) }} · {{ getGroundingLabel(resource.groundingStatus) }}
											</text>
										</view>
									</view>
									<text v-if="!isFileResource(resource) && resource.summary" class="ai-resource-card__summary">{{ resource.summary }}</text>
									<safe-markdown
										v-if="resource.renderer === 'content' && resource.payload.content"
										class="ai-resource-card__content"
										:content="resource.payload.content"
									/>
									<view v-if="resource.renderer === 'business_card'" class="ai-resource-card__business">
										<text
											v-for="field in getBusinessResourceFields(resource)"
											:key="`${resource.key}-${field.label}`"
											class="ai-resource-card__business-field"
										>{{ field.label }}：{{ field.value }}</text>
									</view>
									<image
										v-if="resource.renderer === 'image' && getResourceDisplayPath(resource)"
										class="ai-resource-card__media"
										:src="getResourceDisplayPath(resource)"
										mode="aspectFill"
										@click="previewResourceImage(resource, message)"
									/>
									<video
										v-else-if="resource.renderer === 'video' && getResourceDisplayPath(resource)"
										class="ai-resource-card__media ai-resource-card__video"
										:src="getResourceDisplayPath(resource)"
										controls
										@play="reportResourceInteraction(resource, message, 'preview')"
									></video>
									<view
										v-else-if="resource.renderer === 'audio' && getResourceDisplayPath(resource)"
										class="ai-resource-card__audio"
										@click="toggleResourceAudio(resource, message)"
									>
										<text>{{ isResourceAudioPlaying(resource) ? '停止播放' : '播放音频' }}</text>
									</view>
									<text v-if="resource.unavailable" class="ai-resource-card__unavailable">旧资源链接已失效</text>
									<view v-if="!isFileResource(resource) && getResourceActions(resource).length" class="ai-resource-card__actions">
										<view
											v-for="action in getResourceActions(resource)"
											:key="`${resource.key}-${action.type}`"
											class="ai-resource-card__action"
										:class="{ 'ai-resource-card__action--disabled': action.disabled }"
										@click="handleResourceAction(resource, action, message)"
									>{{ action.disabled ? `${action.label}（暂不可用）` : isResourceLoading(resource) ? '处理中' : action.label }}</view>
									</view>
								</view>
							</view>

							<view v-if="getEvidenceSummary(message).state !== 'pending'" class="ai-evidence-panel" :class="`ai-evidence-panel--${getEvidenceSummary(message).state}`">
								<view class="ai-evidence-panel__header" @click="toggleEvidence(message)">
									<view class="ai-evidence-panel__heading">
										<text class="ai-evidence-panel__title">来源与依据</text>
										<text class="ai-evidence-panel__summary">{{ getEvidenceSummary(message).label }}</text>
									</view>
									<text v-if="getEvidenceSources(message).length" class="ai-evidence-panel__toggle">
										{{ isEvidenceExpanded(message) ? '收起' : '查看' }}
									</text>
								</view>
								<text v-if="getEvidenceMeta(message)" class="ai-evidence-panel__meta">{{ getEvidenceMeta(message) }}</text>
								<view v-if="isEvidenceExpanded(message)" class="ai-evidence-panel__sources">
									<view
										v-for="source in getEvidenceSources(message)"
										:key="source.evidenceId"
										class="ai-evidence-source"
									>
										<text class="ai-evidence-source__title">{{ source.title || '未命名来源' }}</text>
										<text v-if="source.excerpt" class="ai-evidence-source__excerpt">{{ source.excerpt }}</text>
									</view>
								</view>
							</view>
						</view>
						<text v-else class="ai-message-content">{{ message.content }}</text>

					</view>
				</view>

				<view id="ai-message-anchor"></view>
			</scroll-view>

			<view class="ai-assistant-panel__quick">
				<view
					v-for="prompt in quickPrompts"
					:key="prompt"
					class="ai-assistant-panel__quick-chip"
					@click="useQuickPrompt(prompt)"
				>
					{{ prompt }}
				</view>
			</view>

			<view class="ai-assistant-panel__composer">
				<textarea
					v-model="inputValue"
					class="ai-assistant-panel__textarea"
					placeholder="直接问 Leader 智能助手"
					maxlength="2000"
					:auto-height="false"
					confirm-type="send"
					:confirm-hold="true"
					:disabled="sending"
					@confirm="handleInputConfirm"
					@keydown.enter="handleEnterKey"
				/>
				<view class="ai-assistant-panel__composer-bottom">
					<text class="ai-assistant-panel__tip">{{ sending ? '正在思考...' : '输入问题后按回车发送' }}</text>
					<view
						class="ai-assistant-panel__send"
						:class="{ 'ai-assistant-panel__send--disabled': !canSend }"
						@click="sendMessage"
					>
						发送
					</view>
				</view>
			</view>
		</view>

		<view
			v-if="imagePreviewPath"
			class="ai-image-preview"
			@tap.stop="closeImagePreview"
		>
			<view class="ai-image-preview__stage" @tap.stop>
				<image
					class="ai-image-preview__image"
					:src="imagePreviewPath"
					mode="aspectFit"
				/>
				<view class="ai-image-preview__close" @tap.stop="closeImagePreview">×</view>
			</view>
		</view>

		<view
			class="ai-assistant-fab"
			:class="{
				'ai-assistant-fab--collapsed': fabCollapsed,
				'ai-assistant-fab--left': fabDockSide === 'left',
				'ai-assistant-fab--right': fabDockSide === 'right'
			}"
			:style="fabStyle"
			@tap.stop="handleFabTap"
			@touchstart.stop="onTouchStart"
			@touchmove.stop.prevent="onTouchMove"
			@touchend.stop="onTouchEnd"
			@mousedown.stop="onMouseDown"
		>
			<image
				class="ai-assistant-fab__mascot"
				src="/static/ai/mascot-yaya.png"
				mode="aspectFit"
			/>
			<text class="ai-assistant-fab__name">问芽芽</text>
		</view>
	</view>
</template>

<script>
import SafeMarkdown from '@/components/safe-markdown/safe-markdown.vue'
import { ASSISTANT_PUBLIC_RESOURCE_HOSTS, BASE_URL } from '@/utils/config.js'
import {
	downloadAssistantResource,
	getLeaderSessionDetail,
	queryLeaderAgent,
	streamLeaderAgent,
	submitAssistantResourceInteraction
} from '@/api/ai.js'
import {
	buildResourceInteractionRequest,
	countAssistantHits,
	mergeAssistantMessage,
	normalizeAssistantResources,
	normalizeResourceActions,
	resolveAssistantResourceUrl,
	resolveBusinessResourceRoute,
	summarizeEvidenceChain
} from '@/subpackage_ai/assistantMessage.js'

const STORAGE_KEY = 'aiAssistantSessionId'
const RESOURCE_KIND_LABELS = {
	explanation: '知识讲解',
	mind_map: '思维导图',
	diagram: '图表',
	exercise: '练习题',
	code_example: '代码案例',
	extended_reading: '延伸阅读',
	image: '图片',
	video: '视频',
	audio: '音频',
	document: '文档',
	presentation: '演示文稿',
	spreadsheet: '表格',
	bundle: '资料包',
	course: '课程',
	activity: '活动',
	meeting: '会议',
	dining: '餐饮',
	facility: '校园设施',
	secondhand: '二手物品'
}
const RESOURCE_ICON_LABELS = {
	image: '图', video: '影', audio: '音', document: '文', presentation: '演',
	spreadsheet: '表', bundle: '包', content: '答', business_card: '校', generic: '资'
}
const BUSINESS_FIELD_LABELS = {
	teacherName: '教师', weekday: '星期', classroom: '教室', weekText: '周次',
	startTime: '开始', endTime: '结束', location: '地点', status: '状态',
	openingHours: '营业时间', rating: '评分', priceRange: '价格区间',
	price: '价格', condition: '成色', createdAt: '发布时间'
}
const RESOURCE_INTERACTION_BY_ACTION = {
	open_resource: 'open',
	download: 'download',
	preview: 'preview',
	follow_up: 'follow_up'
}

export default {
	components: { SafeMarkdown },
	data() {
		return {
			panelVisible: false,
			sending: false,
			inputValue: '',
			sessionId: '',
			scrollAnchor: 'ai-message-anchor',
			screenWidth: 375,
			screenHeight: 667,
			fabSize: 70,
			fabBottomSpacing: 110,
			defaultFabLeft: 0,
			defaultFabTop: 0,
			fabLeft: 0,
			fabTop: 0,
			fabCollapsed: false,
			fabDockSide: 'right',
			collapseTimer: null,
			dragging: false,
			dragMoved: false,
			dragStartX: 0,
			dragStartY: 0,
			originLeft: 0,
			originTop: 0,
			lastPointerType: '',
			suppressNextTap: false,
			resourceLocalPaths: {},
			resourceLoading: {},
			resourcePreloadFailures: {},
			reportedInteractions: {},
			imagePreviewPath: '',
			audioContext: null,
			activeAudioKey: '',
			viewEpoch: 0,
			localRevision: 0,
			historyRequestGeneration: 0,
			quickPrompts: ['推荐今天的食堂', '我今天有什么课', '校园里有哪些优惠'],
			messages: [
				{
					id: 'welcome',
					role: 'assistant',
					content: '你好，我是 Leader 智能助手。你可以直接问我校园服务、课表、论坛、知识库和常用问题。'
				}
			]
		}
	},
	computed: {
		canSend() {
			return !this.sending && this.inputValue.trim().length > 0
		},
		fabStyle() {
			return {
				left: `${this.fabLeft}px`,
				top: `${this.fabTop}px`,
				zIndex: 1202
			}
		},
		panelStyle() {
			return {
				zIndex: 1201
			}
		}
	},
	watch: {
		messages: {
			deep: true,
			handler() {
				this.$nextTick(() => this.preloadMessageImages())
			}
		}
	},
	created() {
		this.ensureSessionId()
	},
	mounted() {
		this.initFloatingPosition()
		this.scheduleFabCollapse()
	},
	beforeDestroy() {
		this.clearFabCollapseTimer()
		this.removeMouseListeners()
		this.disposeAudio()
	},
	beforeUnmount() {
		this.clearFabCollapseTimer()
		this.removeMouseListeners()
		this.disposeAudio()
	},
	methods: {
		initFloatingPosition() {
			const systemInfo = uni.getSystemInfoSync()
			this.screenWidth = systemInfo.windowWidth || 375
			this.screenHeight = systemInfo.windowHeight || 667
			this.fabSize = 70
			const safeAreaBottomInset = systemInfo.safeArea
				? Math.max(0, this.screenHeight - systemInfo.safeArea.bottom)
				: 0
			this.fabBottomSpacing = Math.max(140, safeAreaBottomInset + 112)
			this.defaultFabLeft = Math.max(12, this.screenWidth - this.fabSize - 16)
			this.defaultFabTop = Math.max(120, this.screenHeight - this.fabSize - this.fabBottomSpacing)
			this.fabLeft = this.defaultFabLeft
			this.fabTop = this.defaultFabTop
			this.updateFabDockSide()
		},
		ensureSessionId() {
			const saved = uni.getStorageSync(STORAGE_KEY)
			if (saved) {
				this.sessionId = saved
				return
			}
			this.sessionId = this.createSessionId()
			uni.setStorageSync(STORAGE_KEY, this.sessionId)
		},
		createSessionId() {
			return `app-ai-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
		},
		togglePanel() {
			const willOpen = !this.panelVisible
			this.panelVisible = willOpen
			this.fabCollapsed = false
			if (willOpen) {
				this.clearFabCollapseTimer()
				this.syncCurrentSession()
			} else {
				this.disposeAudio()
				this.scheduleFabCollapse()
			}
			this.$nextTick(() => {
				this.scrollToBottom()
			})
		},
		closePanel() {
			this.closeImagePreview()
			this.panelVisible = false
			this.disposeAudio()
			this.scheduleFabCollapse()
		},
		openFullConversation() {
			this.ensureSessionId()
			uni.setStorageSync(STORAGE_KEY, this.sessionId)
			this.panelVisible = false
			this.fabCollapsed = false
			this.disposeAudio()
			this.scheduleFabCollapse()
			uni.navigateTo({
				url: `/subpackage_ai/aiConversation/aiConversation?sessionId=${encodeURIComponent(this.sessionId)}`
			})
		},
		resetSession() {
			if (this.sending) return
			this.advanceViewEpoch()
			this.sessionId = this.createSessionId()
			uni.setStorageSync(STORAGE_KEY, this.sessionId)
			this.clearSessionView()
			this.inputValue = ''
		},
		clearSessionView() {
			this.resetResourceState()
			this.messages = [
				{
					id: `welcome-${Date.now()}`,
					role: 'assistant',
					content: '新会话已经开始了。你可以继续问我校园服务、课表、论坛或知识库问题。'
				}
			]
			this.markLocalMutation()
		},
		async syncCurrentSession() {
			const saved = uni.getStorageSync(STORAGE_KEY)
			if (!saved) {
				return
			}
			const sessionChanged = saved !== this.sessionId
			if (sessionChanged) this.advanceViewEpoch()
			this.sessionId = saved
			if (sessionChanged) this.clearSessionView()
			const requestGeneration = ++this.historyRequestGeneration
			const localRevision = this.localRevision
			try {
				const res = await getLeaderSessionDetail(saved)
				if (this.sessionId !== saved
					|| requestGeneration !== this.historyRequestGeneration
					|| localRevision !== this.localRevision) return
				const records = res?.data?.messages || []
				if (!records.length) {
					this.clearSessionView()
					return
				}
				this.resetResourceState()
				this.messages = records.map((item) => {
					const merged = mergeAssistantMessage({}, item)
					return {
						...merged,
						localId: `${item.role}-${item.id}`
					}
				})
				this.markLocalMutation()
				this.scrollToBottom()
			} catch (error) {
				// 会话可能还未产生首条消息，保持当前输入面板即可。
			}
		},
		useQuickPrompt(prompt) {
			this.inputValue = prompt
			this.sendMessage()
		},
		appendMessage(message) {
			const item = {
				localId: `${message.role}-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
				...message
			}
			this.messages.push(item)
			this.markLocalMutation()
			this.scrollToBottom()
			return item
		},
		updateMessage(messageId, patch) {
			const index = this.messages.findIndex(item => this.getMessageKey(item) === messageId)
			if (index === -1) {
				return
			}
			this.messages.splice(index, 1, {
				...this.messages[index],
				...patch
			})
			this.markLocalMutation()
			this.scrollToBottom()
		},
		appendMessageContent(messageId, content) {
			const index = this.messages.findIndex(item => this.getMessageKey(item) === messageId)
			if (index === -1) {
				return
			}
			const current = this.messages[index]
			this.messages.splice(index, 1, {
				...current,
				type: '',
				content: `${current.type === 'thinking' ? '' : current.content || ''}${content}`
			})
			this.markLocalMutation()
			this.scrollToBottom()
		},
		getMessageKey(message) {
			return message?.localId || message?.id || ''
		},
		mergeLiveAssistantEnvelope(messageId, incoming = {}) {
			const index = this.messages.findIndex(item => this.getMessageKey(item) === messageId)
			if (index === -1) return
			const current = this.messages[index]
			const merged = mergeAssistantMessage(current, incoming)
			this.messages.splice(index, 1, {
				...merged,
				localId: current.localId || messageId,
				role: 'assistant'
			})
			this.markLocalMutation()
			this.scrollToBottom()
		},
		markLocalMutation() {
			this.localRevision += 1
		},
		advanceViewEpoch() {
			this.viewEpoch += 1
			this.historyRequestGeneration += 1
		},
		captureViewContext(extra = {}) {
			return {
				sessionId: this.sessionId,
				viewEpoch: this.viewEpoch,
				...extra
			}
		},
		isViewContextCurrent(context, requireMessage = false) {
			if (!context || context.viewEpoch !== this.viewEpoch) return false
			if (!requireMessage || !context.messageKey) return true
			return this.messages.some(item => this.getMessageKey(item) === context.messageKey)
		},
		resetResourceState() {
			this.disposeAudio()
			this.resourceLocalPaths = {}
			this.resourceLoading = {}
			this.resourcePreloadFailures = {}
			this.reportedInteractions = {}
			uni.hideLoading?.()
		},
		scrollToBottom() {
			this.$nextTick(() => {
				this.scrollAnchor = ''
				this.$nextTick(() => {
					this.scrollAnchor = 'ai-message-anchor'
				})
			})
		},
		getDisplayContent(message) {
			const content = String(message?.content || '')
			try {
				const parsed = JSON.parse(content)
				if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
					if (typeof parsed.message === 'string') return parsed.message.trim()
					if (normalizeAssistantResources(message).length) return ''
				}
			} catch (error) {
				// Legacy plain text stays visible; resource extraction is centralized in the shared helper.
			}
			return content
		},
		getOutputTypeTags(message) {
			const types = []
			if (Array.isArray(message?.outputTypes)) types.push(...message.outputTypes)
			if (message?.outputType) types.push(message.outputType)
			if (message?.answerType) types.push(message.answerType)
			types.push(this.detectMessageType(message))
			return [...new Set(types.map(type => this.normalizeOutputType(type)).filter(Boolean))]
		},
		detectMessageType(message) {
			const resources = this.getMessageResources(message)
			if (resources.some(item => item.renderer === 'image')) return 'image'
			if (resources.some(item => item.renderer === 'video')) return 'video'
			if (resources.some(item => ['document', 'presentation', 'spreadsheet', 'bundle'].includes(item.renderer))) return 'document'
			if (resources.some(item => ['diagram', 'mind_map'].includes(item.kind))) return 'diagram'
			if (this.containsFormula(message?.content)) return 'formula'
			return 'text'
		},
		normalizeOutputType(type) {
			const value = String(type || '').toLowerCase()
			if (value.includes('image') || value === 'picture') return 'image'
			if (value.includes('video')) return 'video'
			if (/(document|docx|pdf|ppt|excel)/.test(value)) return 'document'
			if (/(diagram|mermaid|mind_map)/.test(value)) return 'diagram'
			if (/(formula|math)/.test(value)) return 'formula'
			return value === 'text' || value.includes('markdown') ? 'text' : ''
		},
		containsFormula(content) {
			return /```(?:math|latex|tex)\b|\$\$[\s\S]+?\$\$|\\\[[\s\S]+?\\\]|\\\([\s\S]+?\\\)|(?:公式|方程)\s*[:：]/i.test(String(content || ''))
		},
		getOutputTypeLabel(type) {
			const labels = { text: '文本', image: '图片', video: '视频', document: '文档', diagram: '图表', formula: '公式' }
			return labels[type] || '文本'
		},
		getMessageResources(message) {
			return normalizeAssistantResources(message)
		},
		getResourceActions(resource) {
			return normalizeResourceActions(resource)
		},
		getResourceIcon(resource) {
			return RESOURCE_ICON_LABELS[resource?.renderer] || RESOURCE_ICON_LABELS.generic
		},
		isFileResource(resource) {
			return ['document', 'presentation', 'spreadsheet', 'bundle'].includes(resource?.renderer)
		},
		getResourceFileExtension(resource) {
			const title = String(resource?.title || '')
			const titleMatch = title.match(/\.([a-z0-9]{1,8})$/i)
			const value = String(resource?.payload?.format || titleMatch?.[1] || 'FILE').replace(/^\./, '').toUpperCase()
			return ({ WORD: 'DOCX', EXCEL: 'XLSX', MARKDOWN: 'MD' })[value] || value.slice(0, 5)
		},
		getResourceFileClass(resource) {
			const extension = this.getResourceFileExtension(resource).toLowerCase()
			if (['doc', 'docx'].includes(extension)) return 'word'
			if (['xls', 'xlsx', 'csv'].includes(extension)) return 'excel'
			if (extension === 'pdf') return 'pdf'
			if (['zip', 'rar', '7z', 'tar', 'gz'].includes(extension)) return 'archive'
			if (['ppt', 'pptx'].includes(extension)) return 'powerpoint'
			return 'text'
		},
		getResourceSizeLabel(resource) {
			const size = Number(resource?.payload?.size || resource?.metadata?.size || 0)
			if (!Number.isFinite(size) || size <= 0) return this.getResourceFileExtension(resource) + ' 文件'
			if (size < 1024) return `${size} B`
			if (size < 1024 * 1024) return `${(size / 1024).toFixed(size >= 10240 ? 0 : 1)} KB`
			return `${(size / (1024 * 1024)).toFixed(1)} MB`
		},
		getPrimaryFileAction(resource) {
			return this.getResourceActions(resource).find((action) => ['download', 'preview', 'open_resource'].includes(action.type)) || null
		},
		openFileResource(resource, message) {
			const action = this.getPrimaryFileAction(resource)
			if (action) this.handleResourceAction(resource, action, message)
		},
		getResourceKindLabel(resource) {
			return RESOURCE_KIND_LABELS[resource?.kind] || '资源'
		},
		getGroundingLabel(status) {
			return { grounded: '有来源', context_only: '上下文生成', model_only: '模型生成' }[status] || '模型生成'
		},
		getBusinessResourceFields(resource) {
			const payload = resource?.payload || {}
			return Object.entries(payload)
				.filter(([key, value]) => BUSINESS_FIELD_LABELS[key] && value !== '' && value !== null && value !== undefined)
				.slice(0, 6)
				.map(([key, value]) => ({ label: BUSINESS_FIELD_LABELS[key], value: String(value) }))
		},
		getResourceLocalPath(resource) {
			return this.resourceLocalPaths[resource?.key] || ''
		},
		getResourceDisplayPath(resource) {
			const localPath = this.getResourceLocalPath(resource)
			if (localPath) return localPath
			if (resource?.authScope !== 'public') return ''
			for (const value of [resource?.previewUrl, resource?.url]) {
				const rawUrl = String(value || '').trim()
				if (!rawUrl || rawUrl.startsWith('/api/')) continue
				const resolved = resolveAssistantResourceUrl(rawUrl, {
					baseUrl: BASE_URL,
					approvedHosts: ASSISTANT_PUBLIC_RESOURCE_HOSTS
				})
				if (resolved) return resolved
			}
			return ''
		},
		isResourceLoading(resource) {
			return Boolean(this.resourceLoading[resource?.key])
		},
		setResourceLoading(resource, loading) {
			if (!resource?.key) return
			const next = { ...this.resourceLoading }
			if (loading) next[resource.key] = true
			else delete next[resource.key]
			this.resourceLoading = next
		},
		rememberResourceLocalPath(resource, filePath) {
			if (!resource?.key || !filePath) return
			this.resourceLocalPaths = { ...this.resourceLocalPaths, [resource.key]: filePath }
		},
		preloadMessageImages() {
			for (const message of this.messages) {
				for (const resource of this.getMessageResources(message)) {
					if (resource.renderer !== 'image'
						|| this.getResourceDisplayPath(resource)
						|| this.isResourceLoading(resource)
						|| this.resourcePreloadFailures[resource.key]
						|| (!resource.url && !resource.previewUrl)) continue
					this.preloadResourceImage(resource, message)
				}
			}
		},
		async preloadResourceImage(resource, message) {
			const viewContext = this.captureViewContext({ messageKey: this.getMessageKey(message) })
			this.setResourceLoading(resource, true)
			try {
				const filePath = await downloadAssistantResource(resource, {
					approvedHosts: ASSISTANT_PUBLIC_RESOURCE_HOSTS
				})
				if (!this.isViewContextCurrent(viewContext, true)) return
				this.rememberResourceLocalPath(resource, filePath)
			} catch (error) {
				if (this.isViewContextCurrent(viewContext, true)) {
					this.resourcePreloadFailures = {
						...this.resourcePreloadFailures,
						[resource.key]: true
					}
				}
			} finally {
				this.setResourceLoading(resource, false)
			}
		},
		previewResourceImage(resource, message) {
			const viewContext = this.captureViewContext()
			const current = this.getResourceDisplayPath(resource)
			if (!current) return
			this.imagePreviewPath = current
			this.reportResourceInteraction(resource, message, 'preview', viewContext)
		},
		closeImagePreview() {
			this.imagePreviewPath = ''
		},
		isResourceAudioPlaying(resource) {
			return Boolean(resource?.key) && resource.key === this.activeAudioKey
		},
		toggleResourceAudio(resource, message = null, viewContext = null) {
			const actionContext = viewContext || this.captureViewContext()
			if (!resource?.key || !this.isViewContextCurrent(actionContext)) return
			if (this.isResourceAudioPlaying(resource)) {
				this.disposeAudio()
				return
			}
			const source = this.getResourceDisplayPath(resource)
			if (!source || typeof uni.createInnerAudioContext !== 'function') {
				uni.showToast({ title: '当前环境无法播放音频', icon: 'none' })
				return
			}
			this.disposeAudio()
			const context = uni.createInnerAudioContext()
			this.audioContext = context
			this.activeAudioKey = resource.key
			context.src = source
			context.onEnded?.(() => {
				if (this.audioContext === context && this.isViewContextCurrent(actionContext)) this.disposeAudio()
			})
			context.onError?.(() => {
				if (this.audioContext !== context || !this.isViewContextCurrent(actionContext)) return
				this.disposeAudio()
				uni.showToast({ title: '音频播放失败', icon: 'none' })
			})
			context.play()
			if (message) this.reportResourceInteraction(resource, message, 'preview', actionContext)
		},
		disposeAudio() {
			const context = this.audioContext
			this.audioContext = null
			this.activeAudioKey = ''
			if (!context) return
			try {
				context.stop?.()
				context.destroy?.()
			} catch (error) {
				// Native contexts may already be disposed during component teardown.
			}
		},
		async handleResourceAction(resource, action, message) {
			if (!resource || !action || resource.unavailable || action.disabled || action.unavailable
				|| this.isResourceLoading(resource)) return
			const messageId = resource?.messageId ?? message?.messageId ?? message?.id
			const actionContext = this.captureViewContext({
				messageId,
				resourceId: resource?.id,
				resourceKey: resource?.key
			})
			if (action.type === 'follow_up') {
				if (!action.prompt || this.sending) return
				this.reportResourceInteraction(resource, message, action.type, actionContext)
				this.inputValue = action.prompt
				this.sendMessage()
				return
			}
			if (action.type === 'open_resource' && resource.renderer === 'business_card') {
				const route = resolveBusinessResourceRoute(resource)
				if (!route) {
					uni.showToast({ title: '该资源暂无可打开页面', icon: 'none' })
					return
				}
				uni.navigateTo({
					url: route,
					success: () => {
						if (this.isViewContextCurrent(actionContext)) {
							this.reportResourceInteraction(resource, message, action.type, actionContext)
						}
					}
				})
				return
			}
			if (!resource.url && !resource.previewUrl) {
				uni.showToast({ title: '资源暂不可用', icon: 'none' })
				return
			}
			this.setResourceLoading(resource, true)
			uni.showLoading({ title: action.type === 'download' ? '下载中...' : '打开中...' })
			try {
				const filePath = await downloadAssistantResource(resource, {
					approvedHosts: ASSISTANT_PUBLIC_RESOURCE_HOSTS
				})
				if (!this.isViewContextCurrent(actionContext)) return
				this.rememberResourceLocalPath(resource, filePath)
				this.openDownloadedResource(resource, filePath, action.type, actionContext)
				this.reportResourceInteraction(resource, message, action.type, actionContext)
			} catch (error) {
				if (!this.isViewContextCurrent(actionContext)) return
				uni.showToast({ title: error?.message || '资源打开失败', icon: 'none' })
			} finally {
				if (this.isViewContextCurrent(actionContext)) {
					this.setResourceLoading(resource, false)
					uni.hideLoading()
				}
			}
		},
		openDownloadedResource(resource, filePath, actionType = 'preview', viewContext = null) {
			if (viewContext && !this.isViewContextCurrent(viewContext)) return
			if (resource.renderer === 'image') {
				this.imagePreviewPath = filePath
				return
			}
			if (resource.renderer === 'video') {
				uni.showToast({ title: '视频已加载，可在卡片中播放', icon: 'none' })
				return
			}
			if (resource.renderer === 'audio') {
				if (actionType === 'download' && typeof uni.saveFile === 'function') {
					uni.saveFile({
						tempFilePath: filePath,
						success: ({ savedFilePath }) => {
							if (viewContext && !this.isViewContextCurrent(viewContext)) return
							this.rememberResourceLocalPath(resource, savedFilePath || filePath)
							uni.showToast({ title: '音频已保存，可在卡片中播放', icon: 'none' })
						},
						fail: () => {
							if (!viewContext || this.isViewContextCurrent(viewContext)) {
								uni.showToast({ title: '音频已临时保存，可在卡片中播放', icon: 'none' })
							}
						}
					})
					return
				}
				this.toggleResourceAudio(resource, null, viewContext)
				return
			}
			if (typeof uni.openDocument === 'function') {
				uni.openDocument({
					filePath,
					showMenu: true,
					fail: () => {
						if (!viewContext || this.isViewContextCurrent(viewContext)) {
							uni.showToast({ title: '文件已下载，当前环境无法预览', icon: 'none' })
						}
					}
				})
				return
			}
			uni.showToast({ title: '文件已下载', icon: 'none' })
		},
		reportResourceInteraction(resource, message, actionType, viewContext = null) {
			const actionContext = viewContext || this.captureViewContext()
			if (!this.isViewContextCurrent(actionContext)) return
			const interaction = RESOURCE_INTERACTION_BY_ACTION[actionType]
			const sessionId = actionContext.sessionId
			const messageId = actionContext.messageId
				?? resource?.messageId ?? message?.messageId ?? message?.id
			const request = buildResourceInteractionRequest(sessionId, messageId, resource?.id, interaction)
			if (!request || this.reportedInteractions[request.dedupeKey]) return
			this.reportedInteractions = { ...this.reportedInteractions, [request.dedupeKey]: true }
			void submitAssistantResourceInteraction(sessionId, messageId, resource.id, interaction).catch((error) => {
				if (!this.isViewContextCurrent(actionContext)) return
				const next = { ...this.reportedInteractions }
				delete next[request.dedupeKey]
				this.reportedInteractions = next
				console.warn('[assistant-resource-interaction]', {
					sessionId: String(sessionId || '').slice(0, 80),
					messageId: String(messageId || '').slice(0, 80),
					resourceId: String(resource.id || '').slice(0, 80),
					action: interaction,
					status: Number(error?.statusCode || error?.status || 0) || 'failed'
				})
			})
		},
		getEvidenceSummary(message) {
			return summarizeEvidenceChain(message?.evidenceChain)
		},
		getEvidenceMeta(message) {
			const summary = this.getEvidenceSummary(message)
			return [summary.agent, summary.generatedAt].filter(Boolean).join(' · ')
		},
		getEvidenceSources(message) {
			const summary = this.getEvidenceSummary(message)
			return summary.trusted && Array.isArray(message?.evidenceChain?.sources)
				? message.evidenceChain.sources.slice(0, 8)
				: []
		},
		toggleEvidence(message) {
			if (!this.getEvidenceSources(message).length) return
			const key = this.getMessageKey(message)
			const index = this.messages.findIndex(item => this.getMessageKey(item) === key)
			if (index === -1) return
			this.messages.splice(index, 1, { ...this.messages[index], evidenceExpanded: !this.messages[index].evidenceExpanded })
			this.scrollToBottom()
		},
		isEvidenceExpanded(message) {
			return Boolean(message?.evidenceExpanded) && this.getEvidenceSources(message).length > 0
		},
		getTouchPoint(event) {
			const touch = event.touches && event.touches[0]
			if (touch) {
				return { x: touch.clientX, y: touch.clientY }
			}
			const changedTouch = event.changedTouches && event.changedTouches[0]
			if (changedTouch) {
				return { x: changedTouch.clientX, y: changedTouch.clientY }
			}
			return { x: 0, y: 0 }
		},
		startDrag(x, y) {
			this.clearFabCollapseTimer()
			this.fabCollapsed = false
			this.dragging = true
			this.dragMoved = false
			this.dragStartX = x
			this.dragStartY = y
			this.originLeft = this.fabLeft
			this.originTop = this.fabTop
		},
		updateDrag(x, y) {
			if (!this.dragging) {
				return
			}
			const deltaX = x - this.dragStartX
			const deltaY = y - this.dragStartY
			if (Math.abs(deltaX) > 3 || Math.abs(deltaY) > 3) {
				this.dragMoved = true
			}
			this.fabLeft = this.clamp(this.originLeft + deltaX, 0, Math.max(0, this.screenWidth - this.fabSize))
			this.fabTop = this.clamp(this.originTop + deltaY, 0, Math.max(0, this.screenHeight - this.fabSize))
			this.updateFabDockSide()
		},
		endDrag() {
			if (!this.dragging) {
				return
			}
			if (this.dragMoved) {
				this.suppressNextTap = true
				const edgeLeft = 0
				const edgeRight = Math.max(0, this.screenWidth - this.fabSize)
				this.fabLeft = this.fabLeft + this.fabSize / 2 < this.screenWidth / 2 ? edgeLeft : edgeRight
				this.updateFabDockSide()
				if (!this.panelVisible) {
					this.scheduleFabCollapse()
				}
			}
			this.dragging = false
			this.dragMoved = false
		},
		handleFabTap() {
			if (this.suppressNextTap) {
				this.suppressNextTap = false
				return
			}
			if (this.fabCollapsed) {
				this.fabCollapsed = false
				this.scheduleFabCollapse()
				return
			}
			this.togglePanel()
		},
		onTouchStart(event) {
			this.lastPointerType = 'touch'
			const point = this.getTouchPoint(event)
			this.startDrag(point.x, point.y)
		},
		onTouchMove(event) {
			const point = this.getTouchPoint(event)
			this.updateDrag(point.x, point.y)
		},
		onTouchEnd() {
			const moved = this.dragMoved
			this.endDrag()
			if (!moved) {
				this.suppressNextTap = false
			}
		},
		onMouseDown(event) {
			this.lastPointerType = 'mouse'
			this.startDrag(event.clientX, event.clientY)
			this.suppressNextTap = false
			document.addEventListener('mousemove', this.onMouseMove)
			document.addEventListener('mouseup', this.onMouseUp)
		},
		onMouseMove(event) {
			this.updateDrag(event.clientX, event.clientY)
		},
		onMouseUp() {
			this.removeMouseListeners()
			this.endDrag()
		},
		removeMouseListeners() {
			if (typeof document === 'undefined') {
				return
			}
			document.removeEventListener('mousemove', this.onMouseMove)
			document.removeEventListener('mouseup', this.onMouseUp)
		},
		clearFabCollapseTimer() {
			if (!this.collapseTimer) {
				return
			}
			clearTimeout(this.collapseTimer)
			this.collapseTimer = null
		},
		scheduleFabCollapse() {
			this.clearFabCollapseTimer()
			if (this.panelVisible || this.dragging) {
				return
			}
			this.collapseTimer = setTimeout(() => {
				this.fabCollapsed = true
				this.collapseTimer = null
			}, 1800)
		},
		updateFabDockSide() {
			this.fabDockSide = this.fabLeft + this.fabSize / 2 < this.screenWidth / 2 ? 'left' : 'right'
		},
		resetFabPosition() {
			this.fabLeft = this.defaultFabLeft
			this.fabTop = this.defaultFabTop
			this.updateFabDockSide()
		},
		clamp(value, min, max) {
			return Math.min(Math.max(value, min), max)
		},
		handleInputConfirm() {
			this.sendMessage()
		},
		handleEnterKey(event) {
			if (event?.shiftKey) {
				return
			}
			event?.preventDefault?.()
			this.sendMessage()
		},
		async sendMessage() {
			const text = this.inputValue.trim()
			if (!text || this.sending || !this.canSend) {
				return
			}

			this.appendMessage({
				role: 'user',
				content: text
			})
			this.inputValue = ''
			this.sending = true
			const assistantMessage = this.appendMessage({
				role: 'assistant',
				type: 'thinking',
				content: '思考中'
			})
			const assistantMessageId = this.getMessageKey(assistantMessage)
			const requestContext = this.captureViewContext({ messageKey: assistantMessageId })
			const isRequestCurrent = () => this.isViewContextCurrent(requestContext, true)
			let streamStarted = false
			let streamTouched = false

			try {
				const requestPayload = {
					sessionId: requestContext.sessionId,
					prompt: '你是智慧校园 App 的 Leader 智能助手，只负责意图识别、路由和汇总回答。',
					agentName: 'leader_agent',
					input: text
				}

				await streamLeaderAgent(requestPayload, {
					onEvent: (eventName, payload) => {
						if (!isRequestCurrent()) return
						streamTouched = true
						const eventPayload = payload && typeof payload === 'object' && !Array.isArray(payload)
							? payload
							: {}
						const current = this.messages.find(item => this.getMessageKey(item) === assistantMessageId)
						const existingTrace = Array.isArray(current?.trace) ? current.trace : []
						const trace = Array.isArray(eventPayload.trace)
							? eventPayload.trace
							: [...existingTrace, { stage: eventPayload.stage || eventName || 'status', detail: eventPayload }].slice(-24)
						if (eventName === 'generation_start') {
							streamStarted = true
							this.syncSessionId(eventPayload.sessionId)
							this.mergeLiveAssistantEnvelope(assistantMessageId, {
								...eventPayload,
								role: 'assistant',
								type: '',
								content: eventPayload.answer || '已开始生成图片，完成后会自动更新。',
								outputMeta: { ...(eventPayload.outputMeta || {}), generationStatus: 'running' },
								trace
							})
							return
						}
						this.mergeLiveAssistantEnvelope(assistantMessageId, { ...eventPayload, trace })
					},
					onSession: (payload) => {
						if (!isRequestCurrent()) return
						streamTouched = true
						this.syncSessionId(payload?.sessionId)
						this.mergeLiveAssistantEnvelope(assistantMessageId, payload || {})
					},
					onSearch: (payload) => {
						if (!isRequestCurrent()) return
						streamTouched = true
						const hasMatchedResults = Object.prototype.hasOwnProperty.call(payload || {}, 'matchedResults')
							&& Array.isArray(payload?.matchedResults)
						const matchedResultsPatch = hasMatchedResults ? { matchedResults: payload.matchedResults } : {}
						const current = this.messages.find(item => this.getMessageKey(item) === assistantMessageId)
						const retrievalMeta = { ...(current?.retrievalMeta || {}), ...(payload?.retrievalMeta || {}) }
						const trace = [
							...(Array.isArray(current?.trace) ? current.trace : []),
							{
								stage: 'retrieve',
								detail: {
									keyword: payload?.searchKeyword || '',
									matchedCount: countAssistantHits({ ...matchedResultsPatch, retrievalMeta })
								}
							}
						].slice(-24)
						this.mergeLiveAssistantEnvelope(assistantMessageId, {
							...(payload || {}),
							...matchedResultsPatch,
							retrievalMeta,
							trace
						})
					},
					onDelta: (content) => {
						if (!content || !isRequestCurrent()) return
						streamTouched = true
						streamStarted = true
						this.appendMessageContent(assistantMessageId, content)
					},
					onDone: (payload) => {
						if (!isRequestCurrent()) return
						streamTouched = true
						this.syncSessionId(payload?.sessionId)
						const current = this.messages.find(item => this.getMessageKey(item) === assistantMessageId)
						const finalContent = payload?.answer || (current?.type === 'thinking' ? '' : current?.content || '')
						if (!String(finalContent).trim()) {
							throw new Error('模型未返回内容')
						}
						const merged = mergeAssistantMessage(current, {
							...(payload || {}),
							...(Array.isArray(payload?.resources) && payload.resources.length === 0
								&& !Object.prototype.hasOwnProperty.call(payload || {}, 'attachments')
								? { attachments: [] }
								: {}),
							role: 'assistant',
							type: '',
							content: finalContent
						})
						this.updateMessage(assistantMessageId, merged)
					},
					onError: (payload) => {
						if (!isRequestCurrent()) return
						streamTouched = true
						const streamError = new Error(payload?.message || '流式请求失败')
						streamError.payload = payload || {}
						throw streamError
					}
				})
			} catch (error) {
				if (!isRequestCurrent()) return
				if (error?.fallbackToNormalRequest && !streamStarted && !streamTouched) {
					await this.sendMessageFallback(text, assistantMessageId, error, requestContext)
				} else {
					this.messages = this.messages.filter(item => this.getMessageKey(item) !== assistantMessageId)
					uni.showToast({
						title: (error && (error.msg || error.message)) || '模型回复失败',
						icon: 'none'
					})
				}
			} finally {
				this.sending = false
			}
		},
		async sendMessageFallback(text, assistantMessageId, streamError, requestContext) {
			if (!this.isViewContextCurrent(requestContext, true)) return
			try {
				const res = await queryLeaderAgent({
					sessionId: requestContext.sessionId,
					prompt: '你是智慧校园 App 的 Leader 智能助手，只负责意图识别、路由和汇总回答。',
					agentName: 'leader_agent',
					input: text
				})
				if (!this.isViewContextCurrent(requestContext, true)) return
				const payload = res.data || {}
				if (!String(payload.answer || '').trim()) {
					throw new Error('模型未返回内容')
				}
				this.syncSessionId(payload.sessionId)
				const current = this.messages.find(item => this.getMessageKey(item) === assistantMessageId)
				const merged = mergeAssistantMessage(current, {
					...(payload || {}),
					...(Array.isArray(payload?.resources) && payload.resources.length === 0
						&& !Object.prototype.hasOwnProperty.call(payload || {}, 'attachments')
						? { attachments: [] }
						: {}),
					role: 'assistant',
					type: '',
					content: payload.answer
				})
				this.updateMessage(assistantMessageId, merged)
			} catch (error) {
				if (!this.isViewContextCurrent(requestContext, true)) return
				this.messages = this.messages.filter(item => this.getMessageKey(item) !== assistantMessageId)
				uni.showToast({
					title: (error && (error.msg || error.message)) || streamError?.message || '模型回复失败',
					icon: 'none'
				})
			}
		},
		syncSessionId(sessionId) {
			if (!sessionId || sessionId === this.sessionId) {
				return
			}
			this.sessionId = sessionId
			uni.setStorageSync(STORAGE_KEY, sessionId)
		},
	}
}
</script>

<style lang="scss">
.ai-assistant-root {
	position: relative;
	z-index: 1200;
}

.ai-assistant-mask {
	position: fixed;
	inset: 0;
	background: rgba(20, 32, 48, 0.18);
	backdrop-filter: blur(8px);
	z-index: 1200;
}

.ai-image-preview {
	position: fixed;
	inset: 0;
	z-index: 1300;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 32rpx;
	box-sizing: border-box;
	background: rgba(10, 18, 22, 0.82);
	backdrop-filter: blur(6px);
}

.ai-image-preview__stage {
	position: relative;
	width: 100%;
	height: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
}

.ai-image-preview__image {
	width: 100%;
	height: 100%;
}

.ai-image-preview__close {
	position: absolute;
	top: max(12rpx, env(safe-area-inset-top));
	right: 8rpx;
	width: 72rpx;
	height: 72rpx;
	border-radius: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
	background: rgba(255, 255, 255, 0.16);
	border: 2rpx solid rgba(255, 255, 255, 0.48);
	color: #ffffff;
	font-size: 46rpx;
	line-height: 1;
}

.ai-assistant-fab {
	position: fixed;
	width: 140rpx;
	height: 158rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	cursor: pointer;
	transition: transform 0.22s ease, opacity 0.22s ease;
	filter: drop-shadow(0 12rpx 16rpx rgba(20, 92, 73, 0.20));
}

.ai-assistant-fab--collapsed.ai-assistant-fab--right {
	transform: translateX(0);
}

.ai-assistant-fab--collapsed.ai-assistant-fab--left {
	transform: translateX(0);
}

.ai-assistant-fab--collapsed .ai-assistant-fab__name {
	opacity: 0;
}

.ai-assistant-fab__mascot {
	width: 130rpx;
	height: 130rpx;
	display: block;
}

.ai-assistant-fab__name {
	margin-top: -22rpx;
	padding: 6rpx 18rpx;
	border-radius: 999rpx;
	background: #16856b;
	color: #ffffff;
	font-size: 20rpx;
	font-weight: 900;
	line-height: 1;
	white-space: nowrap;
	transition: opacity 0.2s ease;
}

.ai-assistant-panel {
	position: fixed;
	top: 16px;
	right: 12px;
	bottom: 16px;
	left: 12px;
	width: 360px;
	max-width: calc(100vw - 24px);
	height: 540px;
	max-height: calc(100vh - 32px);
	margin: auto;
	box-sizing: border-box;
	background:
		linear-gradient(180deg, rgba(248, 253, 250, 0.98), rgba(239, 248, 243, 0.98)),
		#ffffff;
	border-radius: 36rpx;
	border: 2rpx solid rgba(22, 133, 107, 0.1);
	box-shadow: 0 24rpx 56rpx rgba(20, 92, 73, 0.14);
	display: flex;
	flex-direction: column;
	overflow: hidden;
	transform: translateY(18rpx) scale(0.96);
	opacity: 0;
	transition: all 0.22s ease;
	min-height: 0;
}

.ai-assistant-panel--active {
	transform: translateY(0) scale(1);
	opacity: 1;
}

.ai-assistant-panel__header {
	padding: 26rpx 28rpx 22rpx;
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	background:
		radial-gradient(circle at top right, rgba(255, 255, 255, 0.14), transparent 30%),
		linear-gradient(135deg, #176a57, #16856b 78%);
	color: #fff;
	flex-shrink: 0;
}

.ai-assistant-panel__header > view:first-child {
	flex: 1;
	min-width: 0;
}

.ai-assistant-panel__eyebrow {
	display: block;
	font-size: 20rpx;
	opacity: 0.76;
	text-transform: uppercase;
	letter-spacing: 2rpx;
}

.ai-assistant-panel__title {
	display: block;
	margin-top: 8rpx;
	font-size: 34rpx;
	font-weight: 700;
}

.ai-assistant-panel__actions {
	display: flex;
	align-items: center;
	gap: 10rpx;
	flex-shrink: 0;
}

.ai-assistant-panel__icon,
.ai-assistant-panel__close {
	min-width: 64rpx;
	height: 56rpx;
	padding: 0 14rpx;
	box-sizing: border-box;
	border-radius: 28rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: rgba(255, 255, 255, 0.2);
	font-size: 22rpx;
}

.ai-assistant-panel__close {
	font-size: 34rpx;
	padding-bottom: 4rpx;
}

.ai-assistant-panel__messages {
	flex: 1;
	width: 100%;
	min-height: 0;
	padding: 24rpx 22rpx 18rpx;
	box-sizing: border-box;
	background:
		linear-gradient(180deg, rgba(255, 255, 255, 0.38), rgba(255, 255, 255, 0)),
		transparent;
}

.ai-message-row {
	display: flex;
	margin-bottom: 18rpx;
}

.ai-message-row--user {
	justify-content: flex-end;
}

.ai-message-row--assistant {
	justify-content: flex-start;
}

.ai-message-bubble {
	min-width: 0;
	max-width: 82%;
	padding: 20rpx 22rpx;
	border-radius: 26rpx;
	box-sizing: border-box;
}

.ai-message-bubble--assistant {
	background: rgba(255, 255, 255, 0.96);
	border: 2rpx solid rgba(22, 133, 107, 0.06);
	box-shadow: 0 10rpx 24rpx rgba(20, 92, 73, 0.06);
}

.ai-message-bubble--user {
	background: linear-gradient(135deg, #176a57, #16856b);
	color: #ffffff;
	box-shadow: 0 12rpx 26rpx rgba(22, 133, 107, 0.16);
}

.ai-message-content {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
	font-size: 27rpx;
	line-height: 1.72;
	word-break: break-all;
}

.ai-output-type-list {
	display: flex;
	flex-wrap: wrap;
	gap: 8rpx;
	margin-bottom: 10rpx;
}

.ai-output-type-tag {
	padding: 4rpx 10rpx;
	border-radius: 999rpx;
	background: #e8f5ee;
	color: #176a57;
	font-size: 19rpx;
	font-weight: 700;
}

.ai-message-image {
	width: 360rpx;
	max-width: 100%;
	height: 220rpx;
	margin-bottom: 12rpx;
	border-radius: 16rpx;
	background: #eef2f7;
}

.ai-message-line {
	display: flex;
	align-items: flex-start;
	min-height: 34rpx;
}

.ai-message-line + .ai-message-line {
	margin-top: 8rpx;
}

.ai-message-line--empty {
	min-height: 18rpx;
}

.ai-message-line__dot {
	flex: 0 0 10rpx;
	width: 10rpx;
	height: 10rpx;
	margin-top: 18rpx;
	margin-right: 12rpx;
	border-radius: 50%;
	background: #16856b;
}

.ai-message-line__text {
	flex: 1;
	font-size: 27rpx;
	line-height: 1.72;
	color: inherit;
	word-break: break-all;
}

.ai-resource-list {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

.ai-resource-card {
	display: flex;
	flex-direction: column;
	gap: 10rpx;
	padding: 14rpx;
	border-radius: 16rpx;
	background: #f4faf6;
	border: 1rpx solid rgba(22, 133, 107, 0.14);
}

.ai-resource-card--business_card {
	background: linear-gradient(145deg, #f0f9f3, #ffffff);
}

.ai-resource-card--unavailable {
	opacity: 0.72;
}

.ai-resource-card__header {
	display: flex;
	align-items: center;
	gap: 12rpx;
}

.ai-resource-file {
	display: flex;
	align-items: center;
	gap: 16rpx;
	min-height: 104rpx;
}

.ai-resource-file--clickable {
	cursor: pointer;
}

.ai-resource-file__icon {
	position: relative;
	width: 72rpx;
	height: 92rpx;
	flex-shrink: 0;
	overflow: hidden;
	border-radius: 7rpx 15rpx 7rpx 7rpx;
	background: #64748b;
}

.ai-resource-file__fold {
	position: absolute;
	top: 0;
	right: 0;
	width: 22rpx;
	height: 22rpx;
	background: rgba(255, 255, 255, 0.72);
	clip-path: polygon(0 0, 100% 100%, 0 100%);
}

.ai-resource-file__extension {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 14rpx;
	color: #fff;
	text-align: center;
	font-size: 17rpx;
	font-weight: 900;
}

.ai-resource-file__icon--word { background: #2878d0; }
.ai-resource-file__icon--excel { background: #16865b; }
.ai-resource-file__icon--pdf { background: #e44848; }
.ai-resource-file__icon--archive { background: #7a5cc7; }
.ai-resource-file__icon--powerpoint { background: #d66b2c; }
.ai-resource-file__icon--text { background: #64748b; }

.ai-resource-file__body {
	min-width: 0;
	flex: 1;
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.ai-resource-file__name {
	color: #172033;
	font-size: 24rpx;
	font-weight: 700;
	line-height: 1.35;
	word-break: break-all;
}

.ai-resource-file__meta {
	color: #8490a3;
	font-size: 19rpx;
}

.ai-resource-card__icon {
	width: 48rpx;
	height: 48rpx;
	border-radius: 12rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: #e8f5ee;
	color: #16856b;
	font-size: 19rpx;
	font-weight: 800;
	flex-shrink: 0;
}

.ai-resource-card__heading {
	min-width: 0;
	flex: 1;
	display: flex;
	flex-direction: column;
}

.ai-resource-card__title {
	font-size: 24rpx;
	font-weight: 800;
	color: #263244;
	word-break: break-all;
}

.ai-resource-card__meta,
.ai-resource-card__summary,
.ai-resource-card__content,
.ai-resource-card__business-field {
	font-size: 20rpx;
	line-height: 1.5;
	color: #687990;
}

.ai-resource-card__summary,
.ai-resource-card__content {
	white-space: pre-wrap;
	word-break: break-all;
}

.ai-resource-card__content {
	max-height: 180rpx;
	overflow: hidden;
	color: #344256;
}

.ai-resource-card__business {
	display: flex;
	flex-wrap: wrap;
	gap: 8rpx 14rpx;
}

.ai-resource-card__business-field {
	max-width: 100%;
	padding: 5rpx 10rpx;
	border-radius: 10rpx;
	background: rgba(255, 255, 255, 0.88);
}

.ai-resource-card__media {
	width: 100%;
	height: 210rpx;
	border-radius: 14rpx;
	background: #e8edf4;
	overflow: hidden;
}

.ai-resource-card__video {
	background: #111827;
}

.ai-resource-card__audio {
	min-height: 58rpx;
	padding: 0 16rpx;
	border-radius: 14rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: #e8f5ee;
	color: #176a57;
	font-size: 22rpx;
	font-weight: 800;
}

.ai-resource-card__unavailable {
	font-size: 20rpx;
	color: #a45b00;
}

.ai-resource-card__actions {
	display: flex;
	flex-wrap: wrap;
	gap: 8rpx;
}

.ai-resource-card__action {
	min-height: 46rpx;
	padding: 0 15rpx;
	border-radius: 999rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: #e8f5ee;
	color: #16856b;
	font-size: 20rpx;
	font-weight: 800;
}

.ai-resource-card__action--disabled {
	opacity: 0.58;
	pointer-events: none;
}

.ai-evidence-panel {
	padding: 12rpx 14rpx;
	border-radius: 14rpx;
	background: #f5f9f6;
	border: 1rpx solid rgba(100, 116, 139, 0.12);
}

.ai-evidence-panel--available {
	background: #f3faf6;
	border-color: rgba(24, 160, 88, 0.16);
}

.ai-evidence-panel--malformed,
.ai-evidence-panel--integrity_failed,
.ai-evidence-panel--generation_failed {
	background: #fff5f5;
	border-color: rgba(229, 72, 77, 0.18);
}

.ai-evidence-panel__header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12rpx;
}

.ai-evidence-panel__heading {
	min-width: 0;
	flex: 1;
	display: flex;
	flex-direction: column;
}

.ai-evidence-panel__title,
.ai-evidence-source__title {
	font-size: 21rpx;
	font-weight: 800;
	color: #263244;
}

.ai-evidence-panel__summary,
.ai-evidence-panel__meta,
.ai-evidence-source__excerpt {
	font-size: 19rpx;
	line-height: 1.45;
	color: #68768a;
}

.ai-evidence-panel__toggle {
	font-size: 19rpx;
	font-weight: 800;
	color: #16856b;
}

.ai-evidence-panel__meta {
	display: block;
	margin-top: 6rpx;
}

.ai-evidence-panel__sources {
	display: flex;
	flex-direction: column;
	gap: 8rpx;
	margin-top: 10rpx;
}

.ai-evidence-source {
	display: flex;
	flex-direction: column;
	gap: 3rpx;
	padding-top: 8rpx;
	border-top: 1rpx solid rgba(100, 116, 139, 0.1);
}

.ai-thinking-indicator {
	display: flex;
	align-items: center;
	gap: 14rpx;
	min-height: 38rpx;
}

.ai-thinking-text {
	font-size: 25rpx;
	color: #6b8479;
}

.ai-thinking-dots {
	display: flex;
	align-items: center;
	gap: 8rpx;
}

.ai-thinking-dots text {
	width: 10rpx;
	height: 10rpx;
	border-radius: 50%;
	background: #77b59b;
	animation: ai-thinking-bounce 1s ease-in-out infinite;
}

.ai-thinking-dots text:nth-child(2) {
	animation-delay: 0.16s;
}

.ai-thinking-dots text:nth-child(3) {
	animation-delay: 0.32s;
}

.ai-assistant-panel__quick {
	padding: 0 22rpx 10rpx;
	display: flex;
	flex-wrap: nowrap;
	gap: 12rpx;
	flex-shrink: 0;
	align-items: center;
}

.ai-assistant-panel__quick-chip {
	flex: 1 1 0;
	min-width: 0;
	padding: 10rpx 12rpx;
	border-radius: 999rpx;
	background: rgba(225, 243, 233, 0.96);
	color: #176a57;
	font-size: 21rpx;
	text-align: center;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}

.ai-assistant-panel__composer {
	padding: 16rpx 22rpx 22rpx;
	background: rgba(255, 255, 255, 0.9);
	border-top: 2rpx solid rgba(22, 133, 107, 0.06);
	backdrop-filter: blur(14px);
	flex-shrink: 0;
}

.ai-assistant-panel__textarea {
	width: 100%;
	height: 176rpx;
	min-height: 176rpx;
	max-height: 176rpx;
	padding: 18rpx 20rpx;
	box-sizing: border-box;
	overflow-y: auto;
	background: linear-gradient(180deg, #ffffff, #f7fbf8);
	border-radius: 22rpx;
	border: 2rpx solid rgba(22, 133, 107, 0.12);
	font-size: 28rpx;
	line-height: 1.5;
}

.ai-assistant-panel__composer-bottom {
	margin-top: 16rpx;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 18rpx;
}

.ai-assistant-panel__tip {
	flex: 1;
	font-size: 20rpx;
	line-height: 1.5;
	color: #6b8479;
}

.ai-assistant-panel__send {
	min-width: 132rpx;
	height: 72rpx;
	border-radius: 999rpx;
	background: linear-gradient(135deg, #176a57, #16856b);
	color: #fff;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 26rpx;
	font-weight: 700;
	box-shadow: 0 14rpx 24rpx rgba(22, 133, 107, 0.18);
}

.ai-assistant-panel__send--disabled {
	opacity: 0.45;
}

@keyframes ai-fab-pulse {
	0%, 100% {
		transform: scale(1);
	}
	50% {
		transform: scale(1.05);
	}
}

@keyframes ai-thinking-bounce {
	0%, 80%, 100% {
		opacity: 0.35;
		transform: translateY(0);
	}
	40% {
		opacity: 1;
		transform: translateY(-6rpx);
	}
}
</style>
