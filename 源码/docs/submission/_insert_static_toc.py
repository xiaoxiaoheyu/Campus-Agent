# -*- coding: utf-8 -*-
"""Replace TOC area with a competition-style dotted-leader directory."""
import re
from pathlib import Path

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Pt

DOC_PATH = Path(r"e:\zzs\github\AgentA3\docs\submission\作品报告-AgentA3.docx")
OUT_DESKTOP = Path(r"c:\Users\zzs\Desktop\视频剪辑\文档\作品报告-AgentA3-含目录.docx")

# Page numbers from user's Word TOC screenshot + continuation for later sections
TOC_ENTRIES = [
    (1, "第1章  作品概述", "4"),
    (2, "1.1 建设背景与目标", "4"),
    (2, "1.2 作品定位", "4"),
    (2, "1.3 系统组成", "4"),
    (2, "1.4 预期效果", "6"),
    (1, "第2章  任务分析", "7"),
    (2, "2.1 任务拆解", "7"),
    (2, "2.2 目标用户与角色", "7"),
    (2, "2.3 小程序端主要功能", "7"),
    (3, "2.3.1 校园服务", "7"),
    (3, "2.3.2 AI 助手与多模态创作", "10"),
    (3, "2.3.3 个性化学习、题库与考试", "14"),
    (3, "2.3.4 智能会议", "17"),
    (3, "2.3.5 学习辅助工具", "19"),
    (2, "2.4 Web 管理端主要功能", "20"),
    (3, "2.4.1 校园业务治理", "20"),
    (3, "2.4.2 AI 与教学治理", "22"),
    (2, "2.5 功能交互逻辑", "30"),
    (2, "2.6 非功能与边界", "30"),
    (1, "第3章  原理介绍", "31"),
    (2, "3.1 总体技术路线", "31"),
    (2, "3.2 七维画像证据协议", "31"),
    (2, "3.3 Leader 路由与 Catalog", "32"),
    (2, "3.4 课程资源 typed DAG 与 PPT 管线", "33"),
    (2, "3.5 MaxKB 检索与引用式回答", "34"),
    (2, "3.6 会议实时 ASR 与会后智能体链", "35"),
    (2, "3.7 题库生成、组卷与考试反馈", "36"),
    (1, "第4章  系统实现", "38"),
    (2, "4.1 工程结构与协作契约", "38"),
    (2, "4.2 小程序实现要点", "38"),
    (2, "4.3 Java：画像证据汇总（核心）", "39"),
    (2, "4.4 Python：Leader 路由（核心）", "40"),
    (2, "4.5 Python：课程资源 DAG（核心）", "41"),
    (2, "4.6 Java：MaxKB 引用式回答（核心）", "42"),
    (2, "4.7 会议 ASR 与会后链路", "43"),
    (2, "4.8 题库、组卷、编程练习与 PPT", "44"),
    (2, "4.9 管理端治理实现", "45"),
    (1, "第5章  测试分析", "46"),
    (2, "5.1 测试方案", "46"),
    (2, "5.2 测试环境", "46"),
    (2, "5.3 功能性测试与界面证据", "47"),
    (2, "5.4 自动化与边界", "48"),
    (1, "第6章  作品总结", "49"),
    (2, "6.1 作品特色与创新点", "49"),
    (2, "6.2 作品展望", "50"),
    (2, "6.3 结束语", "50"),
    (1, "附录  截图清单", "51"),
]


def set_run_font_elem(r_pr, east_asia="宋体", size_half_points="24", bold=False):
    r_fonts = OxmlElement("w:rFonts")
    r_fonts.set(qn("w:ascii"), "Times New Roman")
    r_fonts.set(qn("w:hAnsi"), "Times New Roman")
    r_fonts.set(qn("w:eastAsia"), east_asia)
    r_pr.append(r_fonts)
    sz = OxmlElement("w:sz")
    sz.set(qn("w:val"), size_half_points)
    r_pr.append(sz)
    sz_cs = OxmlElement("w:szCs")
    sz_cs.set(qn("w:val"), size_half_points)
    r_pr.append(sz_cs)
    if bold:
        r_pr.append(OxmlElement("w:b"))
        r_pr.append(OxmlElement("w:bCs"))


def make_toc_line(level: int, title: str, page: str):
    p = OxmlElement("w:p")
    p_pr = OxmlElement("w:pPr")

    # Style hint as TOC1/2/3 if available — keep custom formatting
    tabs = OxmlElement("w:tabs")
    tab = OxmlElement("w:tab")
    tab.set(qn("w:val"), "right")
    tab.set(qn("w:leader"), "dot")
    tab.set(qn("w:pos"), "9072")  # ~16 cm
    tabs.append(tab)
    p_pr.append(tabs)

    ind = OxmlElement("w:ind")
    if level == 2:
        ind.set(qn("w:left"), "360")
    elif level == 3:
        ind.set(qn("w:left"), "720")
    if level > 1:
        p_pr.append(ind)

    spacing = OxmlElement("w:spacing")
    spacing.set(qn("w:before"), "40")
    spacing.set(qn("w:after"), "40")
    spacing.set(qn("w:line"), "360")
    spacing.set(qn("w:lineRule"), "auto")
    p_pr.append(spacing)
    p.append(p_pr)

    # title + tab
    r1 = OxmlElement("w:r")
    r_pr1 = OxmlElement("w:rPr")
    size = "24" if level == 1 else "21"
    set_run_font_elem(r_pr1, "黑体" if level == 1 else "宋体", size, bold=(level == 1))
    r1.append(r_pr1)
    t1 = OxmlElement("w:t")
    t1.set(qn("xml:space"), "preserve")
    t1.text = title
    r1.append(t1)
    p.append(r1)

    # tab char
    r_tab = OxmlElement("w:r")
    r_tab.append(OxmlElement("w:tab"))
    p.append(r_tab)

    # page number
    r2 = OxmlElement("w:r")
    r_pr2 = OxmlElement("w:rPr")
    set_run_font_elem(r_pr2, "宋体", "21", bold=(level == 1))
    r2.append(r_pr2)
    t2 = OxmlElement("w:t")
    t2.text = page
    r2.append(t2)
    p.append(r2)
    return p


def is_toc_field_para(p) -> bool:
    xml = p._element.xml
    return "TOC \\o" in xml or "TOC \\o" in xml.replace(" ", "") or "w:instrText" in xml and "TOC" in xml


def is_old_simple_toc_item(text: str) -> bool:
    t = text.strip()
    if re.fullmatch(r"第[1-6]章\s+.+", t) and len(t) < 40:
        return True
    if t.startswith("（打开 Word") or t.startswith("(打开 Word"):
        return True
    return False


def main():
    doc = Document(str(DOC_PATH))

    # Locate 目录 title
    toc_title = None
    for p in doc.paragraphs:
        if p.text.strip() in ("目  录", "目录"):
            toc_title = p
            break
    if toc_title is None:
        raise SystemExit("未找到“目录”标题")

    # Style title
    toc_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    toc_title.text = ""
    run = toc_title.add_run("目  录")
    run.bold = True
    run.font.size = Pt(16)
    run.font.name = "黑体"
    r_pr = run._element.get_or_add_rPr()
    r_fonts = r_pr.get_or_add_rFonts()
    r_fonts.set(qn("w:eastAsia"), "黑体")
    r_fonts.set(qn("w:ascii"), "黑体")
    r_fonts.set(qn("w:hAnsi"), "黑体")

    parent = toc_title._element.getparent()
    title_idx = list(parent).index(toc_title._element)

    # Remove content between TOC title and first real chapter heading paragraph
    # Walk following siblings until 第1章 heading that is Heading style / long chapter start of body
    to_remove = []
    reached_body = False
    for child in list(parent)[title_idx + 1 :]:
        tag = child.tag
        if tag != qn("w:p"):
            # stop at first table? unlikely before ch1
            # page break may be in a paragraph
            text = "".join(node.text or "" for node in child.iter(qn("w:t")))
            if re.match(r"^第1章", text.strip()):
                reached_body = True
                break
            continue
        text = "".join(node.text or "" for node in child.iter(qn("w:t"))).strip()
        xml = child.xml if hasattr(child, "xml") else ""
        # detect page break only empty para — keep one then stop removing when chapter found
        if re.match(r"^第1章", text):
            reached_body = True
            break
        # remove TOC field paras, old list, placeholders, blanks in TOC zone
        if (
            not text
            or is_old_simple_toc_item(text)
            or "TOC" in xml
            or text.startswith("（打开")
            or re.match(r"^\d+\.\d+", text)
            or text.startswith("附录")
        ):
            to_remove.append(child)
            continue
        # if unexpected content, stop
        if text and not text.startswith("第"):
            # still in TOC zone if empty-ish
            to_remove.append(child)
            continue
        to_remove.append(child)

    for el in to_remove:
        parent.remove(el)

    # Recompute insert position
    title_idx = list(parent).index(toc_title._element)
    insert_at = title_idx + 1

    # blank line
    blank = OxmlElement("w:p")
    parent.insert(insert_at, blank)
    insert_at += 1

    for level, title, page in TOC_ENTRIES:
        parent.insert(insert_at, make_toc_line(level, title, page))
        insert_at += 1

    # page break after TOC
    pb = OxmlElement("w:p")
    r = OxmlElement("w:r")
    br = OxmlElement("w:br")
    br.set(qn("w:type"), "page")
    r.append(br)
    pb.append(r)
    parent.insert(insert_at, pb)

    doc.save(str(DOC_PATH))
    print("TOC inserted:", DOC_PATH)

    try:
        import shutil

        shutil.copy2(DOC_PATH, OUT_DESKTOP)
        print("copied:", OUT_DESKTOP)
    except Exception as exc:
        print("desktop copy failed:", exc)
        alt = Path(r"c:\Users\zzs\Desktop\作品报告-AgentA3-含目录.docx")
        try:
            shutil.copy2(DOC_PATH, alt)
            print("copied alt:", alt)
        except Exception as exc2:
            print("alt copy failed:", exc2)


if __name__ == "__main__":
    main()
