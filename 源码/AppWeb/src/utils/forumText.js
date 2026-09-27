const BUILTIN_TOPIC_NAMES = {
  1: '热门',
  2: '最新',
  3: '📢公告',
  4: '💰集市',
  5: '😊求助',
  6: '🔑失物',
  7: '💕表白',
  8: '🍟美食',
  9: '🤝搭子',
  10: '📚学习资料',
  11: '🌸影忆青春',
}

export const forumTopicName = (itemOrId, fallback = '') => {
  const id = typeof itemOrId === 'object' ? itemOrId?.id : itemOrId
  return BUILTIN_TOPIC_NAMES[id] || (typeof itemOrId === 'object' ? itemOrId.topicName || itemOrId.name || fallback : fallback)
}
