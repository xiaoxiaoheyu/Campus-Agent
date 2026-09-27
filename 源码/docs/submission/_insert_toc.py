# -*- coding: utf-8 -*-
"""Insert a proper Word TOC (dotted leaders + page numbers) into the work report."""
import re
import sys
from pathlib import Path

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_TAB_ALIGNMENT, WD_TAB_LEADER
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

DOC_PATH = Path(r"e:\zzs\github\AgentA3\docs\submission\作品报告-AgentA3.docx")
DESKTOP_PATH = Path(r"c:\Users\zzs\Desktop\视频剪辑\文档\作品报告-AgentA3-完善版.docx")


def set_run_font(run, name="宋体", size=12, bold=False):
    run.bold = bold
    run.font.size = Pt(size)
    run.font.name = name
    r_pr = run._element.get_or_add_rPr()
    r_fonts = r_pr.get_or_add_rFonts()
    ascii_font = "Times New Roman" if name != "黑体" else "黑体"
    r_fonts.set(qn("w:ascii"), ascii_font)
    r_fonts.set(qn("w:hAnsi"), ascii_font)
    r_fonts.set(qn("w:eastAsia"), name)


def classify_heading(text: str):
    t = text.strip()
    if not t:
        return None
    if re.match(r"^第[1-6]章\s+", t) or t.startswith("附录"):
        return 1
    if re.match(r"^\d+\.\d+\.\d+\s+", t):
        return 3
    if re.match(r"^\d+\.\d+\s+", t):
        return 2
    return None


def ensure_heading_styles(doc: Document):
    """Promote chapter/section title paragraphs to Heading 1/2/3."""
    for para in doc.paragraphs:
        level = classify_heading(para.text)
        if level is None:
            continue
        # skip cover / TOC title
        if para.text.strip() in ("目  录", "目录"):
            continue
        style_name = f"Heading {level}"
        try:
            para.style = doc.styles[style_name]
        except KeyError:
            continue
        # keep Chinese fonts
        for run in para.runs:
            set_run_font(run, "黑体", {1: 14, 2: 12, 3: 12}[level], True)


def clear_simple_toc(doc: Document):
    """Remove the old plain-text TOC list between 目录 title and first chapter."""
    paras = list(doc.paragraphs)
    toc_idx = None
    chap_idx = None
    for i, p in enumerate(paras):
        t = p.text.strip()
        if t in ("目  录", "目录") and toc_idx is None:
            toc_idx = i
        if toc_idx is not None and i > toc_idx and re.match(r"^第1章", t):
            chap_idx = i
            break
    if toc_idx is None or chap_idx is None:
        return toc_idx
    # delete paragraphs strictly between toc title and chapter 1
    for p in paras[toc_idx + 1 : chap_idx]:
        t = p.text.strip()
        # keep page breaks empty paras that might be needed? remove list items only
        if t.startswith("第") and "章" in t and len(t) < 40:
            el = p._element
            el.getparent().remove(el)
        elif not t:
            # leave one blank maybe
            pass
    return toc_idx


def insert_toc_field_after(paragraph):
    """Insert TOC field paragraph after given paragraph element."""
    parent = paragraph._element.getparent()
    idx = list(parent).index(paragraph._element)

    def make_toc_para():
        p = OxmlElement("w:p")
        # begin
        r1 = OxmlElement("w:r")
        fc1 = OxmlElement("w:fldChar")
        fc1.set(qn("w:fldCharType"), "begin")
        r1.append(fc1)
        p.append(r1)
        # instr
        r2 = OxmlElement("w:r")
        instr = OxmlElement("w:instrText")
        instr.set(qn("xml:space"), "preserve")
        instr.text = ' TOC \\o "1-3" \\h \\z \\u '
        r2.append(instr)
        p.append(r2)
        # separate
        r3 = OxmlElement("w:r")
        fc2 = OxmlElement("w:fldChar")
        fc2.set(qn("w:fldCharType"), "separate")
        r3.append(fc2)
        p.append(r3)
        # placeholder
        r4 = OxmlElement("w:r")
        t = OxmlElement("w:t")
        t.text = "（打开 Word 后请右键目录 → 更新域 → 更新整个目录）"
        r4.append(t)
        p.append(r4)
        # end
        r5 = OxmlElement("w:r")
        fc3 = OxmlElement("w:fldChar")
        fc3.set(qn("w:fldCharType"), "end")
        r5.append(fc3)
        p.append(r5)
        return p

    # hint paragraph
    hint = OxmlElement("w:p")
    hr = OxmlElement("w:r")
    ht = OxmlElement("w:t")
    ht.text = ""
    hr.append(ht)
    hint.append(hr)

    toc_p = make_toc_para()
    parent.insert(idx + 1, hint)
    parent.insert(idx + 2, toc_p)
    return toc_p


def add_static_toc_fallback(doc: Document, after_para):
    """
    Insert a fully visible TOC with dotted leaders (no need to wait for field update).
    Page numbers are approximate placeholders updated later by Word field if present.
    """
    # Prefer letting Word field handle it; also add a formatted static TOC
    # that mirrors competition style, in case field isn't updated yet.
    entries = [
        (1, "第1章  作品概述", ""),
        (2, "1.1 建设背景与目标", ""),
        (2, "1.2 作品定位", ""),
        (2, "1.3 系统组成", ""),
        (2, "1.4 预期效果", ""),
        (1, "第2章  任务分析", ""),
        (2, "2.1 任务拆解", ""),
        (2, "2.2 目标用户与角色", ""),
        (2, "2.3 小程序端主要功能", ""),
        (3, "2.3.1 校园服务", ""),
        (3, "2.3.2 AI 助手与多模态创作", ""),
        (3, "2.3.3 个性化学习、题库与考试", ""),
        (3, "2.3.4 智能会议", ""),
        (3, "2.3.5 学习辅助工具", ""),
        (2, "2.4 Web 管理端主要功能", ""),
        (3, "2.4.1 校园业务治理", ""),
        (3, "2.4.2 AI 与教学治理", ""),
        (2, "2.5 功能交互逻辑", ""),
        (2, "2.6 非功能与边界", ""),
        (1, "第3章  原理介绍", ""),
        (2, "3.1 总体技术路线", ""),
        (2, "3.2 七维画像证据协议", ""),
        (2, "3.3 Leader 路由与 Catalog", ""),
        (2, "3.4 课程资源 typed DAG 与 PPT 管线", ""),
        (2, "3.5 MaxKB 检索与引用式回答", ""),
        (2, "3.6 会议实时 ASR 与会后智能体链", ""),
        (2, "3.7 题库生成、组卷与考试反馈", ""),
        (1, "第4章  系统实现", ""),
        (2, "4.1 工程结构与协作契约", ""),
        (2, "4.2 小程序实现要点", ""),
        (2, "4.3 Java：画像证据汇总（核心）", ""),
        (2, "4.4 Python：Leader 路由（核心）", ""),
        (2, "4.5 Python：课程资源 DAG（核心）", ""),
        (2, "4.6 Java：MaxKB 引用式回答（核心）", ""),
        (2, "4.7 会议 ASR 与会后链路", ""),
        (2, "4.8 题库、组卷、编程练习与 PPT", ""),
        (2, "4.9 管理端治理实现", ""),
        (1, "第5章  测试分析", ""),
        (2, "5.1 测试方案", ""),
        (2, "5.2 测试环境", ""),
        (2, "5.3 功能性测试与界面证据", ""),
        (2, "5.4 自动化与边界", ""),
        (1, "第6章  作品总结", ""),
        (2, "6.1 作品特色与创新点", ""),
        (2, "6.2 作品展望", ""),
        (2, "6.3 结束语", ""),
        (1, "附录  截图清单", ""),
    ]

    parent = after_para._element.getparent()
    idx = list(parent).index(after_para._element)
    insert_at = idx + 1

    # Remove any leftover old simple list between toc and chapter — already handled

    created = []
    for level, title, page in entries:
        p = OxmlElement("w:p")
        pPr = OxmlElement("w:pPr")
        # tabs: right-aligned dotted leader at ~15.5cm
        tabs = OxmlElement("w:tabs")
        tab = OxmlElement("w:tab")
        tab.set(qn("w:val"), "right")
        tab.set(qn("w:leader"), "dot")
        tab.set(qn("w:pos"), "9072")  # ~16cm in twips-ish; 9072 twips ≈ 16cm
        tabs.append(tab)
        pPr.append(tabs)
        ind = OxmlElement("w:ind")
        if level == 2:
            ind.set(qn("w:left"), "420")
        elif level == 3:
            ind.set(qn("w:left"), "840")
        if len(ind.attrib):
            pPr.append(ind)
        spacing = OxmlElement("w:spacing")
        spacing.set(qn("w:line"), "360")
        spacing.set(qn("w:lineRule"), "auto")
        spacing.set(qn("w:after"), "40")
        pPr.append(spacing)
        p.append(pPr)

        r = OxmlElement("w:r")
        rPr = OxmlElement("w:rPr")
        rFonts = OxmlElement("w:rFonts")
        rFonts.set(qn("w:ascii"), "Times New Roman")
        rFonts.set(qn("w:hAnsi"), "Times New Roman")
        rFonts.set(qn("w:eastAsia"), "宋体")
        rPr.append(rFonts)
        sz = OxmlElement("w:sz")
        sz.set(qn("w:val"), "24" if level == 1 else "21")
        rPr.append(sz)
        if level == 1:
            b = OxmlElement("w:b")
            rPr.append(b)
        r.append(rPr)
        t = OxmlElement("w:t")
        t.set(qn("xml:space"), "preserve")
        # page left empty; Word TOC field is preferred. For static, use PAGEREF later.
        t.text = title + "\t"
        r.append(t)
        p.append(r)

        # page number run (filled by COM update if possible; else blank then filled)
        r2 = OxmlElement("w:r")
        rPr2 = OxmlElement("w:rPr")
        rFonts2 = OxmlElement("w:rFonts")
        rFonts2.set(qn("w:ascii"), "Times New Roman")
        rFonts2.set(qn("w:hAnsi"), "Times New Roman")
        rFonts2.set(qn("w:eastAsia"), "宋体")
        rPr2.append(rFonts2)
        sz2 = OxmlElement("w:sz")
        sz2.set(qn("w:val"), "21")
        rPr2.append(sz2)
        r2.append(rPr2)
        t2 = OxmlElement("w:t")
        t2.text = page if page else " "
        r2.append(t2)
        p.append(r2)

        parent.insert(insert_at, p)
        created.append((level, title, p))
        insert_at += 1

    return created


def update_toc_with_word(path: Path) -> bool:
    """Use Word COM to generate/update TOC with real page numbers."""
    try:
        import win32com.client  # type: ignore
    except ImportError:
        print("pywin32 not available")
        return False

    word = None
    doc = None
    try:
        word = win32com.client.DispatchEx("Word.Application")
        word.Visible = False
        word.DisplayAlerts = 0
        abs_path = str(path.resolve())
        doc = word.Documents.Open(abs_path)

        # Ensure headings exist then rebuild TOC
        # Delete existing TOCs
        while doc.TablesOfContents.Count > 0:
            doc.TablesOfContents(1).Delete()

        # Insert TOC at beginning after 目录 heading — find range
        find = doc.Content.Find
        find.ClearFormatting()
        found = find.Execute(FindText="目  录")
        if not found:
            find.Execute(FindText="目录")

        # Go to end of that paragraph
        rng = doc.Content
        # Use Selection approach
        word.Selection.HomeKey(Unit=6)  # wdStory
        find2 = word.Selection.Find
        find2.ClearFormatting()
        ok = find2.Execute(FindText="目  录") or find2.Execute(FindText="目录")
        if ok:
            word.Selection.EndKey(Unit=5)  # wdLine
            word.Selection.MoveRight()
            # Insert paragraph break then TOC
            word.Selection.TypeParagraph()
            toc_range = word.Selection.Range
            toc = doc.TablesOfContents.Add(
                Range=toc_range,
                UseHeadingStyles=True,
                UpperHeadingLevel=1,
                LowerHeadingLevel=3,
                UseHyperlinks=True,
                IncludePageNumbers=True,
                RightAlignPageNumbers=True,
            )
            # dotted leaders
            try:
                toc.TabLeader = 1  # wdTabLeaderDots
            except Exception:
                pass
            toc.Update()

        # Update all fields
        doc.Fields.Update()
        if doc.TablesOfContents.Count > 0:
            doc.TablesOfContents(1).Update()

        doc.Save()
        print("Word TOC updated successfully")
        return True
    except Exception as exc:
        print("Word COM failed:", exc)
        return False
    finally:
        try:
            if doc is not None:
                doc.Close(False)
        except Exception:
            pass
        try:
            if word is not None:
                word.Quit()
        except Exception:
            pass


def main():
    if not DOC_PATH.exists():
        print("missing", DOC_PATH)
        sys.exit(1)

    doc = Document(str(DOC_PATH))
    ensure_heading_styles(doc)

    # find 目录 paragraph
    toc_para = None
    for p in doc.paragraphs:
        if p.text.strip() in ("目  录", "目录"):
            toc_para = p
            set_run_font(p.runs[0] if p.runs else p.add_run("目  录"), "黑体", 16, True)
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            break

    # remove old simple chapter-only list
    clear_simple_toc(doc)

    # Re-find toc para after deletions
    toc_para = None
    for p in doc.paragraphs:
        if p.text.strip() in ("目  录", "目录"):
            toc_para = p
            break

    if toc_para is None:
        # insert at start of body after cover page break — fallback
        toc_para = doc.paragraphs[min(6, len(doc.paragraphs) - 1)]

    # Remove any previous TOC fields near toc
    # Insert Word TOC field
    insert_toc_field_after(toc_para)

    # Style TOC title
    for p in doc.paragraphs:
        if p.text.strip() in ("目  录", "目录"):
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            if not p.runs:
                set_run_font(p.add_run("目  录"), "黑体", 16, True)
            else:
                for r in p.runs:
                    set_run_font(r, "黑体", 16, True)
            break

    doc.save(str(DOC_PATH))
    print("saved styled headings + TOC field ->", DOC_PATH)

    ok = update_toc_with_word(DOC_PATH)
    if not ok:
        print("NOTE: Open the docx in Word/WPS, right-click the TOC, Update Field.")

    try:
        import shutil

        shutil.copy2(DOC_PATH, DESKTOP_PATH)
        print("copied", DESKTOP_PATH)
        if ok:
            update_toc_with_word(DESKTOP_PATH)
    except Exception as exc:
        print("desktop copy/update:", exc)


if __name__ == "__main__":
    main()
