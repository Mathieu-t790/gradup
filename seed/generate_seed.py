#!/usr/bin/env python3
"""Deterministic seed generator for GradUp -> seed/seed_neon.sql

Generates a realistic, self-contained dataset for the GradUp demo:
4 cohorts, ~205 students, full 3-year curriculum (30 credits / semester),
3 exams per finalized offering, ~15k grades, semester validations,
grade histories, disputes and a few transcripts.

The seed targets the shared Neon database: it first DELETES all existing
data (keeping the admin created by AdminBootstrap) then loads everything.

Diplomas are NOT inserted: they are created by the app endpoint
POST /cohorts/{cohortId}/diplomas/generate during the demo.

Run:  python3 seed/generate_seed.py
"""
import random
import uuid
from datetime import date
from decimal import Decimal, ROUND_HALF_UP

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

NS = uuid.UUID("0f1e2d3c-4b5a-4c6d-8e7f-9a8b7c6d5e4f")
PASSWORD_HASH = "$2b$10$BGZ.GTOFHzZrgDiBCo.AZ.9ibgoBze4UfRfY58EitnP44V3BjVuzq"
ADMIN_EMAIL = "admin@hei.school"
STUDENT_DOMAIN = "mail.hei.school"
STAFF_DOMAIN = "hei.school"
# Real email used to test the transcript PDF email sending
TAFITA_EMAIL = "hei.tafita.2@gmail.com"

# Exams per finalized offering: CC1 (1/4), CC2 (1/4), Final (1/2) -> sum = 1
EXAM_WEIGHTS = [(1, 4), (1, 4), (1, 2)]
EXAM_LABELS = ["CC1", "CC2", "Final"]
IN_PROGRESS_WEIGHTS = [(1, 4)]  # single CC1 exam, grading not finalized


def uid(key: str) -> str:
    return str(uuid.uuid5(NS, key))


# ---------------------------------------------------------------------------
# Curriculum (from UE_selon_semestre.md, rebalanced so every semester = 30)
# ---------------------------------------------------------------------------

COMMON = {
    1: [
        ("WEB1", "Web Interfaces", 6),
        ("PROG1", "Algorithms", 8),
        ("SYS1", "Operating Systems", 8),
        ("DONNEES1", "Structured Databases", 4),
        ("MGT1", "Collaborative Work", 4),
    ],
    2: [
        ("WEB2", "Globally Connected Web Interfaces", 8),
        ("PROG2", "Object-Oriented Programming", 6),
        ("SYS2", "Interconnected Systems", 8),
        ("THEORIE1", "Mathematics for Computer Science", 4),
        ("LV1", "Foreign Language - French", 4),
    ],
    3: [
        ("WEB3", "Advanced Web Interfaces", 8),
        ("PROG3", "Backend API Implementation", 8),
        ("PRO1", "Professional Life", 3),
        ("DONNEES2", "Data-Intensive Applications", 4),
        ("MGT2", "Project Management", 3),
        ("LV2", "Foreign Language - English", 4),
    ],
    4: [
        ("PROJET1", "Specialization Project", 16),
        ("IA1", "Symbolic and Statistical Artificial Intelligence", 3),
    ],
    5: [
        ("SECU1", "Information Systems Security", 7),
        ("SECU2", "Application Vulnerabilities", 7),
        ("PRO3", "Entrepreneurship", 3),
        ("DONNEES4", "Business Intelligence", 6),
    ],
    6: [("PRO4", "Professional Experience", 30)],
}

EL_ONLY = {
    4: [("PROG4", "Application Quality and Safety", 8), ("PRO2", "Work-Study Program", 3)],
    5: [("MOB1", "Mobile Application Development", 7)],
}

TN_ONLY = {
    4: [
        ("TN1", "Marketing Digital", 3),
        ("TN2", "E-Reputation and Blogging", 3),
        ("METIER1", "ERP and Related Tools", 5),
    ],
    5: [("TN3", "Web SEO", 4), ("TN4", "Advanced Web Techniques", 3)],
}


def courses_for(semester: int, track: str):
    """(reference, title, credits, track_code) list for a semester and track."""
    out = []
    for ref, title, credits in COMMON.get(semester, []):
        out.append((ref, title, credits, None))
    for ref, title, credits in (EL_ONLY if track == "EL" else TN_ONLY).get(semester, []):
        out.append((ref, title, credits, track))
    return out


# ---------------------------------------------------------------------------
# Cohorts / groups
# ---------------------------------------------------------------------------
# group tuple: (reference, final_track, student_count, optional "merge")
COHORTS = [
    {
        "label": "Mpamakilay",
        "entry": 2021,
        "grad": 2024,
        "groups": [("G1", "EL", 40), ("G2", "TN", 20)],
    },
    {
        "label": "Tohindia",
        "entry": 2022,
        "grad": 2025,
        "groups": [("H1", "TN", 25), ("H2", "EL", 25)],
    },
    {
        "label": "Fahazavana",
        "entry": 2023,
        "grad": 2026,
        "groups": [("I1", "EL", 25), ("I2", "EL", 25)],
    },
    {
        "label": "Maminirina",
        "entry": 2024,
        "grad": 2027,
        "groups": [("J1", "EL", 25), ("J2", "EL", 5, "merge"), ("J3", "TN", 15)],
    },
]

# In-progress semesters: grading not finalized, 1 exam, no validation
IN_PROGRESS = {
    ("2025-2026", 6),  # I - S6 (professional experience in progress)
    ("2025-2026", 4),  # J - S4 (current semester)
}


def semester_year_and_position(entry: int, number: int):
    """Academic year label + position (1=Sep-Jan, 2=Feb-Jun) for a program semester."""
    year_index = (number - 1) // 2
    year_label = f"{entry + year_index}-{entry + year_index + 1}"
    return year_label, (number - 1) % 2 + 1


def semester_dates(year_label: str, number: int):
    year_start = int(year_label.split("-")[0])
    _, position = semester_year_and_position(year_start, number)
    if position == 1:
        return date(year_start, 9, 1), date(year_start + 1, 1, 31)
    start = date(year_start + 1, 2, 1)
    end = date(year_start + 1, 6, 30)
    if (year_label, number) in IN_PROGRESS:
        end = date(year_start + 1, 10, 31)
    return start, end


def s4_date(entry: int) -> date:
    """Track/group assignment happens at the start of S4 (Feb of entry+2)."""
    return date(entry + 2, 2, 1)


def cohort_max_semester(cohort) -> int:
    # Program semesters reached by the current 2025-2026 academic year
    # (S6-I and S4-J are in progress): the semester in progress for a cohort
    # entering in `entry` is the even one 2*(2026-entry), capped at 6.
    return min(6, 2 * (2026 - cohort["entry"]))


# ---------------------------------------------------------------------------
# Name pools (deterministic assignment)
# ---------------------------------------------------------------------------

LAST_NAMES = [
    "Rakoto", "Rabe", "Razafy", "Andria", "Randria", "Rasolofoniaina",
    "Ratsimbazafy", "Rakotomalala", "Razanadrakoto", "Rajaonarison",
    "Razanamalala", "Rakotondrainibe", "Andrianjafy", "Ranaivoson",
    "Ravelojaona", "Ramaro", "Razafindrakoto", "Randrianarisoa",
    "Rabearivelo", "Ratsimba", "Rakotondrabe", "Razafimanantsoa",
]
FIRST_NAMES = [
    "Hery", "Mialy", "Tiana", "Lova", "Sariaka", "Nirina", "Fetra", "Mamisoa",
    "Njaka", "Tantely", "Voahangy", "Ando", "Miora", "Fy", "Aina", "Toky",
    "Sitraka", "Nambinina", "Hasina", "Tsanta", "Soa", "Mendrika", "Onja",
    "Fara", "Hanta",
]
TEACHER_SPECIALTIES = [
    "Algorithms and Data Structures",
    "Web Development",
    "Networks and Systems",
    "Databases",
    "Computer Security",
    "Artificial Intelligence",
    "Mobile Development",
    "Project Management",
    "Mathematics",
    "Business intelligence",
]


def email_for(first: str, last: str, domain: str, used: set, ref: str) -> str:
    base = f"{first}.{last}".lower()
    candidate = f"{base}@{domain}"
    if candidate in used:
        # deterministic suffix from the student reference
        suffix = ref[-2:]
        candidate = f"{base}{suffix}@{domain}"
    used.add(candidate)
    return candidate


# ---------------------------------------------------------------------------
# Special cases (calibrated demo scenarios)
# ---------------------------------------------------------------------------

SPECIAL_BASES = {
    "STD21001": 16.50,  # tafita - rank 1 EL
    "STD21005": 14.00,  # twin 1
    "STD21006": 14.00,  # twin 2
    "STD21007": 14.00,  # twin 3
    "STD21009": 10.00,  # eligible, last rank
}
SPECIAL_FIXED_SCORES = {"STD21001": 16.50, "STD21005": 14.00, "STD21006": 14.00, "STD21007": 14.00, "STD21009": 10.00}
NON_ELIGIBLE = "STD21008"  # one course at 8.00 -> below 10 -> excluded from diplomas
NON_ELIGIBLE_COURSE = "MOB1"
NON_ELIGIBLE_SCORE = 8.00
NO_GRADES = "STD21010"  # never graded -> absent from graduation view
# Track switches at the start of S5 (2023-09-01 for cohort G)
TRACK_SWITCHES = {
    "STD21011": ("EL", "TN", date(2023, 9, 1)),
    "STD21012": ("EL", "TN", date(2023, 9, 1)),
    "STD21013": ("TN", "EL", date(2023, 9, 1)),
}
# Grade used for the grade history / resolved dispute demo
HISTORY_STUDENT = "STD21015"
HISTORY_COURSE = "PROG1"
HISTORY_OLD = "12.00"
HISTORY_NEW = "13.50"


# ---------------------------------------------------------------------------
# Build the dataset
# ---------------------------------------------------------------------------

rng = random.Random(2026)

academic_years = {}  # label -> (start, end)
semesters = {}  # (year_label, number) -> uuid
semester_dates_map = {}  # (year_label, number) -> (start, end)
groups = []  # dicts
students = []  # dicts
courses = {}  # reference -> (title, credits, semester, track)
offerings = []  # dicts
exams = []  # dicts
grades = []  # dicts
histories = []  # dicts
disputes = []  # dicts
validations = []  # dicts
transcripts = []  # dicts
transcript_details = []  # dicts
teacher_assignments = []  # dicts

# --- academic years and semesters -----------------------------------------
for cohort in COHORTS:
    entry = cohort["entry"]
    for number in range(1, cohort_max_semester(cohort) + 1):
        year_label, _ = semester_year_and_position(entry, number)
        year_start = int(year_label.split("-")[0])
        academic_years.setdefault(year_label, (date(year_start, 9, 1), date(year_start + 1, 8, 31)))
        semesters.setdefault((year_label, number), uid(f"sem:{year_label}:{number}"))
        semester_dates_map[(year_label, number)] = semester_dates(year_label, number)

# --- tracks ---------------------------------------------------------------
track_uid = {"EL": uid("track:EL"), "TN": uid("track:TN")}

# --- courses --------------------------------------------------------------
for semester in range(1, 7):
    for track in (None, "EL", "TN"):
        for ref, title, credits, _ in courses_for(semester, track if track else "EL"):
            if ref not in courses:
                courses[ref] = (title, credits, semester, track)

# --- groups ---------------------------------------------------------------
for cohort in COHORTS:
    cohort_uid_ = uid(f"cohort:{cohort['label']}")
    for group in cohort["groups"]:
        gref, gtrack, _size, *_merge = group
        groups.append(
            {
                "cohort": cohort["label"],
                "cohort_uid": cohort_uid_,
                "reference": gref,
                "track": gtrack,
                "uid": uid(f"group:{cohort['label']}:{gref}"),
            }
        )

# --- teachers -------------------------------------------------------------
teachers = []
for i in range(10):
    ref = f"TCH210{i + 1:02d}"
    first = FIRST_NAMES[i]
    last = LAST_NAMES[i]
    teachers.append(
        {
            "reference": ref,
            "first_name": first,
            "last_name": last,
            "email": email_for(first, last, STAFF_DOMAIN, set(), ref),
            "uid": uid(f"user:{ref}"),
            "specialty": TEACHER_SPECIALTIES[i],
        }
    )

# --- students -------------------------------------------------------------
student_seq = 0
email_used = set()
for cohort in COHORTS:
    entry = cohort["entry"]
    cohort_uid_ = uid(f"cohort:{cohort['label']}")
    enrollment = date(entry, 9, 1)
    cohort_seq = 0
    for group in cohort["groups"]:
        gref, gtrack, size, *_merge = group
        group_uid = uid(f"group:{cohort['label']}:{gref}")
        for _ in range(size):
            student_seq += 1
            cohort_seq += 1
            ref = f"STD{str(entry)[-2:]}{cohort_seq:03d}"
            if ref == "STD21001":
                first, last = "Tafita", "Mathieu"
                email = TAFITA_EMAIL
            else:
                first = FIRST_NAMES[student_seq % len(FIRST_NAMES)]
                last = LAST_NAMES[student_seq % len(LAST_NAMES)]
                email = email_for(first, last, STUDENT_DOMAIN, email_used, ref)
            email_used.add(email)
            birth_year = entry - 20
            students.append(
                {
                    "reference": ref,
                    "first_name": first,
                    "last_name": last,
                    "email": email,
                    "uid": uid(f"user:{ref}"),
                    "cohort": cohort["label"],
                    "cohort_uid": cohort_uid_,
                    "group": gref,
                    "group_uid": group_uid,
                    "track": gtrack,
                    "entry": entry,
                    "enrollment": enrollment,
                    "dob": date(birth_year, 8, 15 + student_seq % 12),
                }
            )

student_by_ref = {s["reference"]: s for s in students}

# --- offerings, exams, teacher assignments --------------------------------
offering_index = 0
exam_index = 0
by_group_semester = {}  # (group_uid, semester_uid) -> [offering]
exam_by_offering = {}  # offering key -> [exam]
for cohort in COHORTS:
    cohort_uid_ = uid(f"cohort:{cohort['label']}")
    entry = cohort["entry"]
    max_sem = cohort_max_semester(cohort)
    for group in groups:
        if group["cohort"] != cohort["label"]:
            continue
        gref = group["reference"]
        gtrack = group["track"]
        group_uid = group["uid"]
        for number in range(1, max_sem + 1):
            # J2 merges into J1 at S4: no J2 offerings from S4 on
            if gref == "J2" and number >= 4:
                continue
            year_label, _ = semester_year_and_position(entry, number)
            sem_uid = semesters[(year_label, number)]
            for cref, ctitle, ccredits, ctrack in courses_for(number, gtrack):
                offering_index += 1
                offering = {
                    "uid": uid(f"offering:{cref}:{group_uid}:{sem_uid}"),
                    "course": cref,
                    "group_uid": group_uid,
                    "semester_uid": sem_uid,
                    "in_progress": (year_label, number) in IN_PROGRESS,
                }
                offerings.append(offering)
                by_group_semester.setdefault((group_uid, sem_uid), []).append(offering)
                # exams
                weights = IN_PROGRESS_WEIGHTS if offering["in_progress"] else EXAM_WEIGHTS
                labels = ["CC1"] if offering["in_progress"] else EXAM_LABELS
                exams_for_offering = []
                for i, ((num, den), label) in enumerate(zip(weights, labels)):
                    exam_index += 1
                    exam = {
                        "uid": uid(f"exam:{cref}:{group_uid}:{sem_uid}:{i}"),
                        "offering_uid": offering["uid"],
                        "label": label,
                        "weight_numerator": num,
                        "weight_denominator": den,
                    }
                    exams.append(exam)
                    exams_for_offering.append(exam)
                exam_by_offering[offering["uid"]] = exams_for_offering

# teacher assignments: one teacher per offering, round-robin over shuffled list
shuffled_offerings = sorted(o["uid"] for o in offerings)
for i, offering_uid in enumerate(shuffled_offerings):
    teacher = teachers[i % len(teachers)]
    teacher_assignments.append(
        {"offering_uid": offering_uid, "teacher_uid": teacher["uid"]}
    )

# --- student histories (group + track) ------------------------------------
group_histories = []  # (student_uid, group_uid, start, end, reason)
track_histories = []  # (student_uid, track_code, start, end, reason)
for student in students:
    cohort = next(c for c in COHORTS if c["label"] == student["cohort"])
    s4 = s4_date(student["entry"])
    gstart = student["enrollment"]
    gref = student["group"]
    g_uid = student["group_uid"]
    track_switch = TRACK_SWITCHES.get(student["reference"])
    if gref == "J2":
        # merge into J1 at S4
        j1_uid = uid(f"group:Maminirina:J1")
        group_histories.append((student["uid"], g_uid, gstart, s4, "Group too small - merged into J1"))
        group_histories.append((student["uid"], j1_uid, s4, None, None))
        track_histories.append((student["uid"], student["track"], s4, None, None))
    elif track_switch:
        old_track, new_track, switch_date = track_switch
        target_group = "G1" if new_track == "EL" else "G2"
        new_group_uid = uid(f"group:Mpamakilay:{target_group}")
        group_histories.append((student["uid"], g_uid, gstart, switch_date, "Track change"))
        group_histories.append((student["uid"], new_group_uid, switch_date, None, None))
        track_histories.append((student["uid"], old_track, s4, switch_date, "Track change"))
        track_histories.append((student["uid"], new_track, switch_date, None, None))
    else:
        group_histories.append((student["uid"], g_uid, gstart, None, None))
        track_histories.append((student["uid"], student["track"], s4, None, None))


def group_at(student, sem_start: date):
    """Replicates TranscriptScope.groupAt: latest history row active at date."""
    matches = [h for h in group_histories if h[0] == student["uid"] and h[2] <= sem_start and (h[3] is None or h[3] >= sem_start)]
    return max(matches, key=lambda h: h[2]) if matches else None


# --- grades ---------------------------------------------------------------
for student in students:
    if student["reference"] == NO_GRADES:
        continue
    cohort = next(c for c in COHORTS if c["label"] == student["cohort"])
    max_sem = cohort_max_semester(cohort)
    base = SPECIAL_BASES.get(student["reference"], rng.gauss(12.8, 1.7))
    base = max(9.0, min(15.5, base))
    for number in range(1, max_sem + 1):
        year_label, _ = semester_year_and_position(student["entry"], number)
        sem_uid = semesters[(year_label, number)]
        sem_start, _ = semester_dates_map[(year_label, number)]
        hist = group_at(student, sem_start)
        if hist is None:
            continue
        group_uid = hist[1]
        for offering in by_group_semester.get((group_uid, sem_uid), []):
            for exam in exam_by_offering[offering["uid"]]:
                fixed = SPECIAL_FIXED_SCORES.get(student["reference"])
                if fixed is not None:
                    score = fixed
                elif student["reference"] == NON_ELIGIBLE and offering["course"] == NON_ELIGIBLE_COURSE:
                    score = NON_ELIGIBLE_SCORE
                elif (
                    student["reference"] == HISTORY_STUDENT
                    and offering["course"] == HISTORY_COURSE
                    and exam["label"] == "CC1"
                ):
                    score = Decimal(HISTORY_NEW)
                else:
                    score = round(base + rng.uniform(-0.75, 0.75), 2)
                    score = max(0.0, min(20.0, score))
                grades.append(
                    {
                        "uid": uid(f"grade:{student['uid']}:{exam['uid']}"),
                        "student_uid": student["uid"],
                        "exam_uid": exam["uid"],
                        "score": score,
                    }
                )

# --- grade history + disputes ---------------------------------------------
exam_label = {e["uid"]: e["label"] for e in exams}
exam_offering = {e["uid"]: e["offering_uid"] for e in exams}
offering_course = {o["uid"]: o["course"] for o in offerings}
history_grade = next(
    g
    for g in grades
    if g["student_uid"] == student_by_ref[HISTORY_STUDENT]["uid"]
    and offering_course[exam_offering[g["exam_uid"]]] == HISTORY_COURSE
    and exam_label[g["exam_uid"]] == "CC1"
)
history_uid = uid(f"history:{history_grade['uid']}")
histories.append(
    {
        "uid": history_uid,
        "grade_uid": history_grade["uid"],
        "old_score": HISTORY_OLD,
        "new_score": HISTORY_NEW,
        "reason": "Correction after dispute",
    }
)

dispute_student_2 = student_by_ref["STD21016"]
dispute_grade_2 = next(
    g for g in grades if g["student_uid"] == dispute_student_2["uid"]
)
dispute_student_3 = student_by_ref["STD21017"]
dispute_grade_3 = next(
    g for g in grades if g["student_uid"] == dispute_student_3["uid"]
)
disputes.append(
    {
        "uid": uid(f"dispute:1"),
        "grade_uid": history_grade["uid"],
        "student_uid": student_by_ref[HISTORY_STUDENT]["uid"],
        "reason": "Disputing the programming grade",
        "status": "PENDING",
    }
)
disputes.append(
    {
        "uid": uid(f"dispute:2"),
        "grade_uid": dispute_grade_2["uid"],
        "student_uid": dispute_student_2["uid"],
        "reason": "The point total seems wrong to me",
        "status": "PENDING",
    }
)
disputes.append(
    {
        "uid": uid(f"dispute:3"),
        "grade_uid": history_grade["uid"],
        "student_uid": student_by_ref[HISTORY_STUDENT]["uid"],
        "reason": "My paper is worth more than the grade given",
        "status": "RESOLVED",
        "resolution_note": "Grade corrected after reviewing the paper",
        "resulting_history_uid": history_uid,
    }
)
disputes.append(
    {
        "uid": uid(f"dispute:4"),
        "grade_uid": dispute_grade_3["uid"],
        "student_uid": dispute_student_3["uid"],
        "reason": "I submitted a better assignment",
        "status": "REJECTED",
        "resolution_note": "No grading error found",
    }
)

# --- per-student course averages (weighted, like v_course_average) ---------
student_averages = {}  # student_uid -> {offering_uid: Decimal avg}
student_course_offerings = {}  # student_uid -> [offering] attended (for transcripts)
for student in students:
    if student["reference"] == NO_GRADES:
        continue
    avgs = {}
    attended = []
    cohort = next(c for c in COHORTS if c["label"] == student["cohort"])
    max_sem = cohort_max_semester(cohort)
    for number in range(1, max_sem + 1):
        year_label, _ = semester_year_and_position(student["entry"], number)
        sem_uid = semesters[(year_label, number)]
        sem_start, _ = semester_dates_map[(year_label, number)]
        hist = group_at(student, sem_start)
        if hist is None:
            continue
        group_uid = hist[1]
        for offering in by_group_semester.get((group_uid, sem_uid), []):
            exam_list = exam_by_offering[offering["uid"]]
            scores = [
                next(
                    (g["score"] for g in grades if g["student_uid"] == student["uid"] and g["exam_uid"] == e["uid"]),
                    None,
                )
                for e in exam_list
            ]
            if any(s is None for s in scores):
                continue
            weights = IN_PROGRESS_WEIGHTS if offering["in_progress"] else EXAM_WEIGHTS
            num = sum(Decimal(str(s)) * Decimal(n) / Decimal(d) for s, (n, d) in zip(scores, weights))
            den = sum(Decimal(n) / Decimal(d) for n, d in weights)
            avgs[offering["uid"]] = num / den
            attended.append(offering)
    student_averages[student["uid"]] = avgs
    student_course_offerings[student["uid"]] = attended


def competition_rank(sorted_items):
    """Replicates Ranking.competitionRanks (Decimal compareTo semantics)."""
    ranks = {}
    rank = 0
    prev = None
    for i, (key, avg) in enumerate(sorted_items):
        if prev is None or prev != avg:  # Decimal == is scale-insensitive like BigDecimal.compareTo
            rank = i + 1
        ranks[key] = rank
        prev = avg
    return ranks


# --- validations (35 rows; S4-J and S6-I intentionally NOT finalized) ------
for cohort in COHORTS:
    entry = cohort["entry"]
    max_sem = cohort_max_semester(cohort)
    tracks = ["EL", "TN"]
    if cohort["label"] == "Fahazavana":
        tracks = ["EL"]  # only EL groups
    for number in range(1, max_sem + 1):
        if (cohort["label"], number) in {("Fahazavana", 6), ("Maminirina", 4)}:
            continue  # in-progress semester, not finalized
        year_label, _ = semester_year_and_position(entry, number)
        sem_uid = semesters[(year_label, number)]
        for track in tracks:
            validations.append(
                {
                    "uid": uid(f"val:{year_label}:{number}:{track}"),
                    "semester_uid": sem_uid,
                    "track": track,
                }
            )

# --- seeded transcripts ----------------------------------------------------
# tafita: FULL for the 3 years of cohort Mpamakilay
# a couple of PROVISIONAL for in-progress semesters
def transcript_student(ref):
    return student_by_ref[ref]


def full_transcript(student, year_label):
    year_start = int(year_label.split("-")[0])
    numbers = [n for n in range(1, 7) if semester_year_and_position(student["entry"], n)[0] == year_label]
    offerings_in_scope = []
    for number in numbers:
        sem_uid = semesters[(semester_year_and_position(student["entry"], number)[0], number)]
        sem_start, _ = semester_dates_map[(semester_year_and_position(student["entry"], number)[0], number)]
        hist = group_at(student, sem_start)
        if hist is None:
            continue
        offerings_in_scope += by_group_semester.get((hist[1], sem_uid), [])
    avgs = student_averages[student["uid"]]
    total = 0
    acquired = 0
    graded = 0
    weighted = Decimal(0)
    for offering in offerings_in_scope:
        credits = courses[offering["course"]][1]
        avg = avgs.get(offering["uid"])
        total += credits
        if avg is None:
            continue
        graded += credits
        weighted += avg * credits
        if avg >= Decimal(10):
            acquired += credits
    weighted_average = (
        (weighted / graded).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP) if graded else None
    )
    return offerings_in_scope, acquired, total, weighted_average


def provisional_transcript(student, semester_number):
    year_label, _ = semester_year_and_position(student["entry"], semester_number)
    sem_uid = semesters[(year_label, semester_number)]
    sem_start, _ = semester_dates_map[(year_label, semester_number)]
    hist = group_at(student, sem_start)
    offerings_in_scope = by_group_semester.get((hist[1], sem_uid), []) if hist else []
    return offerings_in_scope, sem_uid


tafita = transcript_student("STD21001")
for year_label in ("2021-2022", "2022-2023", "2023-2024"):
    offerings_in_scope, acquired, total, weighted_average = full_transcript(tafita, year_label)
    transcript_uid = uid(f"transcript:{tafita['reference']}:full:{year_label}")
    transcripts.append(
        {
            "uid": transcript_uid,
            "student_uid": tafita["uid"],
            "type": "FULL",
            "academic_year": year_label,
            "overall_average": weighted_average,
            "credits_earned": acquired,
            "storage_key": f"transcripts/{tafita['uid']}/{transcript_uid}.pdf",
            "offerings": offerings_in_scope,
        }
    )

h_student = transcript_student("STD22026")
offerings_in_scope, acquired, total, weighted_average = full_transcript(h_student, "2022-2023")
transcript_uid = uid(f"transcript:{h_student['reference']}:full:2022-2023")
transcripts.append(
    {
        "uid": transcript_uid,
        "student_uid": h_student["uid"],
        "type": "FULL",
        "academic_year": "2022-2023",
        "overall_average": weighted_average,
        "credits_earned": acquired,
        "storage_key": f"transcripts/{h_student['uid']}/{transcript_uid}.pdf",
        "offerings": offerings_in_scope,
    }
)

for ref, sem_number in (("STD23001", 6), ("STD23026", 6), ("STD24001", 4), ("STD24031", 4)):
    student = transcript_student(ref)
    offerings_in_scope, sem_uid = provisional_transcript(student, sem_number)
    transcript_uid = uid(f"transcript:{student['reference']}:provisional:{sem_number}")
    transcripts.append(
        {
            "uid": transcript_uid,
            "student_uid": student["uid"],
            "type": "PROVISIONAL",
            "semester_uid": sem_uid,
            "storage_key": f"transcripts/{student['uid']}/{transcript_uid}.pdf",
            "offerings": offerings_in_scope,
        }
    )

for transcript in transcripts:
    avgs = student_averages[transcript["student_uid"]]
    for offering in transcript["offerings"]:
        avg = avgs.get(offering["uid"])
        transcript_details.append(
            {
                "uid": uid(f"detail:{transcript['uid']}:{offering['uid']}"),
                "transcript_uid": transcript["uid"],
                "offering_uid": offering["uid"],
                "course_score": avg,
                "credits_earned": avg is not None and avg >= Decimal(10),
            }
        )

# ---------------------------------------------------------------------------
# SQL generation
# ---------------------------------------------------------------------------


def q(value) -> str:
    """SQL literal."""
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "TRUE" if value else "FALSE"
    if isinstance(value, int):
        return str(value)
    if isinstance(value, Decimal):
        return str(value)
    if isinstance(value, float):
        return repr(value)
    if isinstance(value, date):
        return f"'{value.isoformat()}'"
    return "'" + str(value).replace("'", "''") + "'"


def values_rows(rows, columns, value_getter):
    """Multi-row VALUES clause."""
    body = []
    for row in rows:
        body.append("(" + ", ".join(q(value_getter(row, col)) for col in columns) + ")")
    return body


def emit_insert(statement, rows, columns, value_getter, chunk=500):
    """Chunked multi-row INSERT ... VALUES."""
    body = values_rows(rows, columns, value_getter)
    out = []
    for i in range(0, len(body), chunk):
        out.append(f"INSERT INTO {statement}\nVALUES\n  " + ",\n  ".join(body[i:i + chunk]) + ";")
    return "\n".join(out)


sql = []
a = sql.append

a("-- =====================================================================")
a("-- GradUp - demo seed")
a("-- Generated by seed/generate_seed.py (deterministic, replayable)")
a("--")
a("-- WARNING: this script DELETES all existing data in the database")
a("-- (except the admin 'admin@hei.school' created by AdminBootstrap).")
a("-- =====================================================================")
a("")
a("BEGIN;")
a("")
a("-- ---------------------------------------------------------------------")
a("-- 1. Cleanup (FK-safe order, admin kept)")
a("-- ---------------------------------------------------------------------")
for table in [
    "grade_dispute",
    "grade_history",
    "grade",
    "transcript_detail",
    "transcript",
    "diploma",
    "exam",
    "teacher_assignment",
    "course_offering",
    "student_track_history",
    "student_group_history",
    "student",
    "course",
    "semester_credit_validation",
    "semester",
    "groups",
    "academic_year",
    "teacher",
    "track",
    "cohort",
]:
    a(f"DELETE FROM {table};")
a("DELETE FROM admin WHERE user_id NOT IN (SELECT user_id FROM users WHERE email = %s);" % q(ADMIN_EMAIL))
a("DELETE FROM users WHERE email <> %s;" % q(ADMIN_EMAIL))
a("DELETE FROM reference_counter;")
a("")
a("-- ---------------------------------------------------------------------")
a("-- 2. Admin (no-op if already created by AdminBootstrap)")
a("-- ---------------------------------------------------------------------")
a(
    "INSERT INTO users (user_id, reference, last_name, first_name, email, password_hash, role, is_active) "
    "VALUES (%s, 'ADM21001', 'Administrator', 'System', %s, %s, 'ADMIN', TRUE) "
    "ON CONFLICT (email) DO NOTHING;" % (q(uid("user:ADM21001")), q(ADMIN_EMAIL), q(PASSWORD_HASH))
)
a(
    "INSERT INTO admin (user_id) "
    "SELECT user_id FROM users WHERE email = %s "
    "ON CONFLICT (user_id) DO NOTHING;" % q(ADMIN_EMAIL)
)
a("")
a("-- ---------------------------------------------------------------------")
a("-- 3. Tracks")
a("-- ---------------------------------------------------------------------")
a(
    emit_insert(
        "track (track_id, code, label)",
        [("EL", "Software Ecosystem"), ("TN", "Digital Transformation")],
        ["track_id", "code", "label"],
        lambda row, col: {"track_id": uid(f"track:{row[0]}"), "code": row[0], "label": row[1]}[col],
    )
)
a("")
a("-- ---------------------------------------------------------------------")
a("-- 4. Cohorts")
a("-- ---------------------------------------------------------------------")
a(
    emit_insert(
        "cohort (cohort_id, label, entry_year, expected_graduation_year)",
        COHORTS,
        ["cohort_id", "label", "entry_year", "expected_graduation_year"],
        lambda row, col: {
            "cohort_id": uid(f"cohort:{row['label']}"),
            "label": row["label"],
            "entry_year": row["entry"],
            "expected_graduation_year": row["grad"],
        }[col],
    )
)
a("")
a("-- ---------------------------------------------------------------------")
a("-- 5. Academic years")
a("-- ---------------------------------------------------------------------")
a(
    emit_insert(
        "academic_year (academic_year_id, label, start_date, end_date)",
        sorted(academic_years.items()),
        ["academic_year_id", "label", "start_date", "end_date"],
        lambda row, col: {
            "academic_year_id": uid(f"ay:{row[0]}"),
            "label": row[0],
            "start_date": row[1][0],
            "end_date": row[1][1],
        }[col],
    )
)
a("")
a("-- ---------------------------------------------------------------------")
a("-- 6. Semesters")
a("-- ---------------------------------------------------------------------")
a(
    emit_insert(
        "semester (semester_id, number, academic_year_id, start_date, end_date)",
        sorted(semesters.items()),
        ["semester_id", "number", "academic_year_id", "start_date", "end_date"],
        lambda row, col: {
            "semester_id": row[1],
            "number": row[0][1],
            "academic_year_id": uid(f"ay:{row[0][0]}"),
            "start_date": semester_dates_map[row[0]][0],
            "end_date": semester_dates_map[row[0]][1],
        }[col],
    )
)
a("")
a("-- ---------------------------------------------------------------------")
a("-- 7. Groups (track_id NULL = common core until S4)")
a("-- ---------------------------------------------------------------------")
a(
    emit_insert(
        "groups (group_id, reference, cohort_id, track_id)",
        groups,
        ["group_id", "reference", "cohort_id", "track_id"],
        lambda row, col: {
            "group_id": row["uid"],
            "reference": row["reference"],
            "cohort_id": row["cohort_uid"],
            "track_id": None,
        }[col],
    )
)
a("")
a("-- Tracks assigned from S4 onward (group references are immutable)")
for cohort in COHORTS:
    s4 = s4_date(cohort["entry"])
    a(f"UPDATE groups SET track_id = CASE reference {''.join('WHEN %s THEN %s::uuid ' % (q(gref), q(track_uid[gtrack])) for gref, gtrack, *_ in cohort['groups'])} END WHERE cohort_id = %s;" % q(uid(f"cohort:{cohort['label']}")))
a("")
a("-- ---------------------------------------------------------------------")
a("-- 8. Courses")
a("-- ---------------------------------------------------------------------")
course_rows = [{"ref": ref, "title": t, "credits": c, "sem": s, "track": tr} for ref, (t, c, s, tr) in courses.items()]
a(
    emit_insert(
        "course (course_id, reference, title, credits, semester_number, track_id)",
        course_rows,
        ["course_id", "reference", "title", "credits", "semester_number", "track_id"],
        lambda row, col: {
            "course_id": uid(f"course:{row['ref']}"),
            "reference": row["ref"],
            "title": row["title"],
            "credits": row["credits"],
            "semester_number": row["sem"],
            "track_id": None if row["track"] is None else track_uid[row["track"]],
        }[col],
    )
)
a("")
a("-- ---------------------------------------------------------------------")
a("-- 9. Course offerings")
a("-- ---------------------------------------------------------------------")
a(
    emit_insert(
        "course_offering (offering_id, course_id, group_id, semester_id, grading_finalized)",
        offerings,
        ["offering_id", "course_id", "group_id", "semester_id", "grading_finalized"],
        lambda row, col: {
            "offering_id": row["uid"],
            "course_id": uid(f"course:{row['course']}"),
            "group_id": row["group_uid"],
            "semester_id": row["semester_uid"],
            "grading_finalized": not row["in_progress"],
        }[col],
    )
)
a("")
a("-- ---------------------------------------------------------------------")
a("-- 10. Exams (3 per finalized course: CC1 1/4, CC2 1/4, Final 1/2)")
a("-- ---------------------------------------------------------------------")
a(
    emit_insert(
        "exam (exam_id, offering_id, label, exam_date, exam_time, weight_numerator, weight_denominator)",
        exams,
        ["exam_id", "offering_id", "label", "exam_date", "exam_time", "weight_numerator", "weight_denominator"],
        lambda row, col: {
            "exam_id": row["uid"],
            "offering_id": row["offering_uid"],
            "label": row["label"],
            "exam_date": None,
            "exam_time": None,
            "weight_numerator": row["weight_numerator"],
            "weight_denominator": row["weight_denominator"],
        }[col],
    )
)
a("")
a("-- ---------------------------------------------------------------------")
a("-- 12. Users (students + teachers)")
a("-- ---------------------------------------------------------------------")
user_rows = []
for s in students:
    user_rows.append((s["uid"], s["reference"], s["last_name"], s["first_name"], s["email"], "STUDENT"))
for t in teachers:
    user_rows.append((t["uid"], t["reference"], t["last_name"], t["first_name"], t["email"], "TEACHER"))
a(
    emit_insert(
        "users (user_id, reference, last_name, first_name, email, password_hash, role, is_active)",
        user_rows,
        ["user_id", "reference", "last_name", "first_name", "email", "password_hash", "role", "is_active"],
        lambda row, col: {
            "user_id": row[0],
            "reference": row[1],
            "last_name": row[2],
            "first_name": row[3],
            "email": row[4],
            "password_hash": PASSWORD_HASH,
            "role": row[5],
            "is_active": True,
        }[col],
    )
)
a("")
a("-- ---------------------------------------------------------------------")
a("-- 11. Teachers (after users, before assignments)")
a("-- ---------------------------------------------------------------------")
a(
    emit_insert(
        "teacher (user_id, specialty)",
        teachers,
        ["user_id", "specialty"],
        lambda row, col: {"user_id": row["uid"], "specialty": row["specialty"]}[col],
    )
)
a("")
a("-- ---------------------------------------------------------------------")
a("-- 12. Teacher assignments (one teacher per course, round-robin)")
a("-- ---------------------------------------------------------------------")
a(
    emit_insert(
        "teacher_assignment (id, offering_id, teacher_id)",
        teacher_assignments,
        ["id", "offering_id", "teacher_id"],
        lambda row, col: {
            "id": uid(f"ta:{row['offering_uid']}:{row['teacher_uid']}"),
            "offering_id": row["offering_uid"],
            "teacher_id": row["teacher_uid"],
        }[col],
    )
)
a("")
a("-- ---------------------------------------------------------------------")
a("-- 13. Students")
a("-- ---------------------------------------------------------------------")
a(
    emit_insert(
        "student (user_id, date_of_birth, cohort_id, enrollment_date)",
        students,
        ["user_id", "date_of_birth", "cohort_id", "enrollment_date"],
        lambda row, col: {
            "user_id": row["uid"],
            "date_of_birth": row["dob"],
            "cohort_id": row["cohort_uid"],
            "enrollment_date": row["enrollment"],
        }[col],
    )
)
a("")
a("-- ---------------------------------------------------------------------")
a("-- 13. Student group history")
a("-- ---------------------------------------------------------------------")
a(
    emit_insert(
        "student_group_history (id, student_id, group_id, start_date, end_date, change_reason)",
        group_histories,
        ["id", "student_id", "group_id", "start_date", "end_date", "change_reason"],
        lambda row, col: {
            "id": uid(f"gh:{row[0]}:{row[1]}:{row[2]}"),
            "student_id": row[0],
            "group_id": row[1],
            "start_date": row[2],
            "end_date": row[3],
            "change_reason": row[4],
        }[col],
    )
)
a("")
a("-- ---------------------------------------------------------------------")
a("-- 14. Student track history")
a("-- ---------------------------------------------------------------------")
a(
    emit_insert(
        "student_track_history (id, student_id, track_id, start_date, end_date, change_reason)",
        track_histories,
        ["id", "student_id", "track_id", "start_date", "end_date", "change_reason"],
        lambda row, col: {
            "id": uid(f"th:{row[0]}:{row[1]}:{row[2]}"),
            "student_id": row[0],
            "track_id": track_uid[row[1]],
            "start_date": row[2],
            "end_date": row[3],
            "change_reason": row[4],
        }[col],
    )
)
a("")
a("-- ---------------------------------------------------------------------")
a("-- 15. Grades")
a("-- ---------------------------------------------------------------------")
a("INSERT INTO grade (grade_id, student_id, exam_id, score, recorded_by)")
a("SELECT g.grade_id::uuid, g.student_id::uuid, g.exam_id::uuid, g.score::numeric, a.user_id")
a("FROM (VALUES")
grade_body = []
for g in grades:
    grade_body.append("(%s, %s, %s, %s)" % (q(g["uid"]), q(g["student_uid"]), q(g["exam_uid"]), q(g["score"])))
a("  " + ",\n  ".join(grade_body))
a(") AS g(grade_id, student_id, exam_id, score)")
a("CROSS JOIN (SELECT user_id FROM users WHERE email = %s) AS a;" % q(ADMIN_EMAIL))
a("")
a("-- ---------------------------------------------------------------------")
a("-- 16. Grade history + disputes")
a("-- ---------------------------------------------------------------------")
a("INSERT INTO grade_history (history_id, grade_id, old_score, new_score, modified_by, reason)")
a("SELECT h.history_id::uuid, h.grade_id::uuid, h.old_score::numeric, h.new_score::numeric, a.user_id, h.reason")
a("FROM (VALUES")
history_body = []
for h in histories:
    history_body.append("(%s, %s, %s, %s, %s)" % (q(h["uid"]), q(h["grade_uid"]), q(h["old_score"]), q(h["new_score"]), q(h["reason"])))
a("  " + ",\n  ".join(history_body))
a(") AS h(history_id, grade_id, old_score, new_score, reason)")
a("CROSS JOIN (SELECT user_id FROM users WHERE email = %s) AS a;" % q(ADMIN_EMAIL))
a("")
a("INSERT INTO grade_dispute (dispute_id, grade_id, student_id, reason, status, resolved_at, resolved_by, resolution_note, resulting_history_id)")
a("SELECT d.dispute_id::uuid, d.grade_id::uuid, d.student_id::uuid, d.reason, d.status, CASE WHEN d.status = 'PENDING' THEN NULL ELSE now() END, CASE WHEN d.status = 'PENDING' THEN NULL ELSE a.user_id END, d.resolution_note, d.resulting_history_id::uuid")
a("FROM (VALUES")
dispute_body = []
for d in disputes:
    dispute_body.append(
        "(%s, %s, %s, %s, %s, %s, %s)"
        % (
            q(d["uid"]),
            q(d["grade_uid"]),
            q(d["student_uid"]),
            q(d["reason"]),
            q(d["status"]),
            q(d.get("resolution_note")),
            q(d.get("resulting_history_uid")),
        )
    )
a("  " + ",\n  ".join(dispute_body))
a(") AS d(dispute_id, grade_id, student_id, reason, status, resolution_note, resulting_history_id)")
a("CROSS JOIN (SELECT user_id FROM users WHERE email = %s) AS a;" % q(ADMIN_EMAIL))
a("")
a("-- ---------------------------------------------------------------------")
a("-- 17. Semester credit validations (S4-J and S6-I not finalized)")
a("-- ---------------------------------------------------------------------")
a("INSERT INTO semester_credit_validation (validation_id, semester_id, track_id, total_credits, validated_by)")
a("SELECT v.validation_id::uuid, v.semester_id::uuid, v.track_id::uuid, 30, a.user_id")
a("FROM (VALUES")
validation_body = []
for v in validations:
    validation_body.append("(%s, %s, %s)" % (q(v["uid"]), q(v["semester_uid"]), q(track_uid[v["track"]])))
a("  " + ",\n  ".join(validation_body))
a(") AS v(validation_id, semester_id, track_id)")
a("CROSS JOIN (SELECT user_id FROM users WHERE email = %s) AS a;" % q(ADMIN_EMAIL))
a("")
a("-- ---------------------------------------------------------------------")
a("-- 18. Transcripts + details (PROVISIONAL / FULL; DIPLOMA generated in demo)")
a("-- ---------------------------------------------------------------------")
transcript_rows = []
for t in transcripts:
    transcript_rows.append(
        {
            "uid": t["uid"],
            "student_uid": t["student_uid"],
            "type": t["type"],
            "semester_uid": t.get("semester_uid"),
            "academic_year": t.get("academic_year"),
            "overall_average": t.get("overall_average"),
            "credits_earned": t.get("credits_earned"),
            "storage_key": t["storage_key"],
        }
    )
a(
    emit_insert(
        "transcript (transcript_id, student_id, type, semester_id, academic_year_id, overall_average, credits_earned, storage_key, recipient_email)",
        transcript_rows,
        ["transcript_id", "student_id", "type", "semester_id", "academic_year_id", "overall_average", "credits_earned", "storage_key", "recipient_email"],
        lambda row, col: {
            "transcript_id": row["uid"],
            "student_id": row["student_uid"],
            "type": row["type"],
            "semester_id": row["semester_uid"],
            "academic_year_id": None if row["academic_year"] is None else uid(f"ay:{row['academic_year']}"),
            "overall_average": row["overall_average"],
            "credits_earned": row["credits_earned"],
            "storage_key": row["storage_key"],
            "recipient_email": student_by_ref[next(s["reference"] for s in students if s["uid"] == row["student_uid"])]["email"],
        }[col],
    )
)
a("")
a(
    emit_insert(
        "transcript_detail (detail_id, transcript_id, offering_id, course_score, credits_earned)",
        transcript_details,
        ["detail_id", "transcript_id", "offering_id", "course_score", "credits_earned"],
        lambda row, col: {
            "detail_id": row["uid"],
            "transcript_id": row["transcript_uid"],
            "offering_id": row["offering_uid"],
            "course_score": row["course_score"],
            "credits_earned": row["credits_earned"],
        }[col],
    )
)
a("")
a("COMMIT;")
a("")
a("-- =====================================================================")
a("-- Checks")
a("-- =====================================================================")
a("SELECT 'users' AS t, count(*) FROM users UNION ALL")
a("SELECT 'students', count(*) FROM student UNION ALL")
a("SELECT 'teachers', count(*) FROM teacher UNION ALL")
a("SELECT 'courses', count(*) FROM course UNION ALL")
a("SELECT 'groups', count(*) FROM groups UNION ALL")
a("SELECT 'offerings', count(*) FROM course_offering UNION ALL")
a("SELECT 'exams', count(*) FROM exam UNION ALL")
a("SELECT 'grades', count(*) FROM grade UNION ALL")
a("SELECT 'validations', count(*) FROM semester_credit_validation UNION ALL")
a("SELECT 'transcripts', count(*) FROM transcript;")
a("")
a("-- Sum of credits per (semester, track) must always be 30")
a("SELECT ay.label, s.number, t.code, SUM(sub.credits) AS credits")
a("FROM (SELECT DISTINCT o.semester_id, g.track_id, c.course_id, c.credits")
a("      FROM course_offering o")
a("      JOIN course c ON c.course_id = o.course_id")
a("      JOIN groups g ON g.group_id = o.group_id) sub")
a("JOIN semester s ON s.semester_id = sub.semester_id")
a("JOIN academic_year ay ON ay.academic_year_id = s.academic_year_id")
a("LEFT JOIN track t ON t.track_id = sub.track_id")
a("GROUP BY ay.label, s.number, t.code")
a("ORDER BY ay.label, s.number, t.code;")

with open("seed/seed_neon.sql", "w") as f:
    f.write("\n".join(sql))

# ---------------------------------------------------------------------------
# Assertions / summary
# ---------------------------------------------------------------------------

print("=== Seed summary ===")
print(f"cohorts        : {len(COHORTS)}")
print(f"academic years : {len(academic_years)}")
print(f"semesters      : {len(semesters)}")
print(f"courses        : {len(courses)}")
print(f"groups         : {len(groups)}")
print(f"students       : {len(students)}")
print(f"teachers       : {len(teachers)}")
print(f"offerings      : {len(offerings)}")
print(f"exams          : {len(exams)}")
print(f"grades         : {len(grades)}")
print(f"validations    : {len(validations)}")
print(f"transcripts    : {len(transcripts)}")
print(f"transcript details: {len(transcript_details)}")
print(f"histories      : {len(histories)}")
print(f"disputes       : {len(disputes)}")

# eligibility + ranks per cohort (replica of v_graduation_eligibility)
print("\n=== Eligibility / ranks (recomputed in Python) ===")
for cohort in COHORTS:
    el_by_track = {}
    for student in students:
        if student["cohort"] != cohort["label"]:
            continue
        avgs = student_averages.get(student["uid"])
        if not avgs:
            continue
        if any(a < Decimal(10) for a in avgs.values()):
            continue
        overall = sum(avgs.values()) / Decimal(len(avgs))
        el_by_track.setdefault(student["track"], []).append((student["reference"], overall))
    for track in ("EL", "TN"):
        items = el_by_track.get(track, [])
        items.sort(key=lambda x: (-x[1], x[0]))
        if cohort["label"] == "Mpamakilay" and track == "EL":
            ranks = competition_rank([(ref, avg) for ref, avg in items])
            tafita_rank = ranks.get("STD21001")
            print(f"Mpamakilay EL: {len(items)} eligible - tafita's rank = {tafita_rank}")
            top = items[:3]
            print(f"  top 3: {[(r, str(avg)) for r, avg in top]}")
        else:
            print(f"{cohort['label']} {track}: {len(items)} eligible - top = {items[0] if items else None}")

non_eligible = student_by_ref[NON_ELIGIBLE]["reference"]
print(f"\nCase: {non_eligible} not eligible (course {NON_ELIGIBLE_COURSE} at 8.00) - {NO_GRADES} has no grades at all - twins STD21005/06/07 - tafita rank 1 EL")
print("File seed/seed_neon.sql generated.")