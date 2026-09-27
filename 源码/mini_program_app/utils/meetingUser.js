import { getUserInfo } from './storage.js'
import { repairMojibake } from './text.js'

export function getCurrentDisplayName() {
  const user = getUserInfo()
  const candidates = [
    user?.realName,
    user?.username,
    user?.personalNumber,
    user?.studentId
  ]
  const name = candidates.find(item => typeof item === 'string' && item.trim())
  return name ? repairMojibake(name.trim()) : ''
}

export function buildMeetingParticipants(extraParticipants = []) {
  const names = [getCurrentDisplayName(), ...extraParticipants]
    .filter(name => typeof name === 'string' && name.trim())
    .map(name => repairMojibake(name.trim()))
  return Array.from(new Set(names))
}

export function toMeetingMembers(participants = [], currentName = getCurrentDisplayName()) {
  const repairedCurrentName = repairMojibake(currentName)
  const seenNames = new Set()
  return participants
    .filter(name => typeof name === 'string' && name.trim())
    .map(name => repairMojibake(name.trim()))
    // 历史乱码姓名和正常姓名修复后可能相同，必须在标准化后再次去重。
    .filter(displayName => {
      if (!displayName || seenNames.has(displayName)) return false
      seenNames.add(displayName)
      return true
    })
    .map((displayName, index) => {
      return {
        name: displayName,
        isSelf: !!repairedCurrentName && displayName === repairedCurrentName,
        className: `avatar-${['a', 'b', 'c', 'd'][index % 4]}`
      }
    })
}
