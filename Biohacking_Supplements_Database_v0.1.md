# Biohacking Supplements / Nootropics Database (Initial Seed)
**Version:** 0.3 – Outcome Domain primary + Mechanism secondary (Oct 2026)  
**Purpose:** Structured starting point for an app database of cognitive / biohacking supplements.  
**Notes:**  
- Effects are generally modest and context-dependent.  
- “Helpful under X” means the strongest evidence or clearest signal appears in that population or condition, not that it is useless elsewhere.  
- Prescription items are clearly marked.  
- Condition-specific claims (including ADHD) appear only where published human evidence is solid. Personal differential responses, unique brain chemistry interactions, or anecdotal patterns belong in individual user logs — not in the shared database.  
- Not medical advice. Always consult a clinician, especially with existing conditions or medications.

## Classification Model
**Primary filter for the app:** Outcome Domain (what the user wants to move).  
**Secondary tags:** Mechanism (how it works — used for stacking logic and interactions).

### Outcome Domains
- **Attention / Focus**
- **Memory**
- **Mental Energy / Fatigue Resistance**
- **Stress Resilience / Mood**
- **Sleep Architecture**
- **Foundational / Nutrient Status**
- **Physical Performance / Recovery**
- **Longevity / Healthspan**

### Mechanism Tags
- Stimulant / Adenosine antagonist
- Cholinergic
- Adaptogen
- Amino acid / Neurotransmitter precursor
- Membrane / Phospholipid support
- Mitochondrial / Energy metabolism
- Cortisol / HPA modulator

## Evidence-Grade Rubric
| Grade | Label | Criteria | Example use in app |
|-------|-------|----------|--------------------|
| **A** | Strong | Multiple high-quality RCTs or meta-analyses with consistent, clinically meaningful effects in the stated population/context. Low risk of bias overall. | Primary recommendation filter |
| **B** | Moderate | Several RCTs or one solid meta-analysis; effects are real but modest, or limited to specific conditions (fatigue, older adults, etc.). Some methodological limits. | Secondary / contextual filter |
| **C** | Limited | Few small trials, mixed results, or high risk of bias. Signal exists but not reliable. | Research / optional tag only |
| **D** | Preliminary | Mostly preclinical, mechanistic, single weak studies, or null results in the relevant population. | Hide by default or “low confidence” |

Grades are context-specific (e.g., creatine = B for memory in older adults or under load, lower for young rested adults).

---

## Data Schema (for future app / structured export)
Each entry follows:
- **Name** / Aliases / Brand examples  
- **Status**: OTC | Prescription  
- **Primary outcome domains** (list)  
- **Mechanism tags** (list)  
- **Best evidence contexts**  
- **Evidence grade** (per rubric, context-aware)  
- **Typical dose** (studied)  
- **Onset**  
- **Key PubMed links**  
- **Safety / notes**

---

## Entries

### Caffeine
- **Status**: OTC  
- **Primary outcome domains**: Attention / Focus, Mental Energy / Fatigue Resistance  
- **Mechanism tags**: Stimulant / Adenosine antagonist  
- **Best evidence contexts**: Healthy adults (acute)  
- **Evidence grade**: A (acute alertness/attention)  
- **Typical dose**: 50–200 mg acute; ≤400 mg/day total  
- **Onset**: 30–60 minutes  
- **Key links**: Camfield et al. 2014 meta (caffeine + theanine) [PMID: 24946991](https://pubmed.ncbi.nlm.nih.gov/24946991/)  
- **Safety**: Well-studied. Tolerance, sleep disruption if late, anxiety/jitter in sensitive people.  
- **Notes**: Best paired with L-theanine for smoother effect.

### L-Theanine
- **Status**: OTC  
- **Primary outcome domains**: Attention / Focus, Stress Resilience / Mood  
- **Mechanism tags**: Amino acid / Neurotransmitter precursor  
- **Best evidence contexts**: Healthy adults (especially + caffeine)  
- **Evidence grade**: B (stronger with caffeine)  
- **Typical dose**: 100–200 mg (often 2:1 theanine:caffeine)  
- **Onset**: Within ~1 hour  
- **Key links**: Camfield 2014 [PMID: 24946991](https://pubmed.ncbi.nlm.nih.gov/24946991/); 2025 meta [PMID: 40314930](https://pubmed.ncbi.nlm.nih.gov/40314930/)  
- **Safety**: Excellent.

### Caffeine + L-Theanine (stack)
- **Status**: OTC  
- **Primary outcome domains**: Attention / Focus  
- **Mechanism tags**: Stimulant / Adenosine antagonist, Amino acid / Neurotransmitter precursor  
- **Best evidence contexts**: Healthy adults, acute laboratory tasks  
- **Evidence grade**: A among natural stacks  
- **Typical dose**: 40–200 mg caffeine + 100–200 mg theanine  
- **Key links**: Camfield et al. 2014 [PMID: 24946991](https://pubmed.ncbi.nlm.nih.gov/24946991/)

### L-Tyrosine
- **Status**: OTC  
- **Primary outcome domains**: Mental Energy / Fatigue Resistance, Attention / Focus  
- **Mechanism tags**: Amino acid / Neurotransmitter precursor  
- **Best evidence contexts**: Sleep deprivation, extreme environments, high cognitive load  
- **Evidence grade**: B (situational)  
- **Typical dose**: 500–2000 mg acute under stress  
- **Onset**: Acute  
- **Safety**: Generally safe at studied doses; caution with thyroid issues or MAOIs.

### Citicoline (CDP-Choline) – Cognizin®
- **Brand**: Cognizin (Kyowa Hakko) most studied branded form  
- **Status**: OTC  
- **Primary outcome domains**: Attention / Focus, Memory  
- **Mechanism tags**: Cholinergic, Membrane / Phospholipid support  
- **Best evidence contexts**: Healthy adults and adolescent males (attention); older adults with mild memory concerns  
- **Evidence grade**: B  
- **Typical dose**: 250–500 mg/day  
- **Onset**: Days to weeks  
- **Key links**: McGlade et al. adolescent males [PMID: 26179181](https://pubmed.ncbi.nlm.nih.gov/26179181/)  
- **Safety**: Excellent; well tolerated.  
- **Notes**: One of the better-documented OTC cholinergics for attention.

### Alpha-GPC
- **Status**: OTC  
- **Primary outcome domains**: Memory, Attention / Focus  
- **Mechanism tags**: Cholinergic  
- **Best evidence contexts**: Mild cognitive impairment (MCI), some dementia-related contexts  
- **Evidence grade**: B in clinical populations; C in healthy younger adults  
- **Typical dose**: 300–600 mg (healthy); higher in older impairment studies  
- **Safety**: Generally good. Large Korean cohort linked prescribed use to higher stroke risk (dose-response) [Lee et al. 2021](https://jamanetwork.com/journals/jamanetworkopen/fullarticle/2786547). Needs replication.  
- **Notes**: More clinical-population oriented than citicoline.

### Bacopa monnieri
- **Status**: OTC  
- **Primary outcome domains**: Memory, Attention / Focus  
- **Mechanism tags**: Adaptogen  
- **Best evidence contexts**: Healthy adults (chronic)  
- **Evidence grade**: B (mixed recent data)  
- **Typical dose**: 300–600 mg standardized extract (≥50% bacosides), with food  
- **Onset**: 8–12 weeks  
- **Key links**: Kongkeaw et al. 2014 meta [PMID: 24252493](https://pubmed.ncbi.nlm.nih.gov/24252493/)  
- **Safety**: Mild GI upset common.  
- **Notes**: Slow-building.

### Rhodiola rosea
- **Status**: OTC  
- **Primary outcome domains**: Mental Energy / Fatigue Resistance, Stress Resilience / Mood  
- **Mechanism tags**: Adaptogen  
- **Best evidence contexts**: Fatigue, stress, night-shift / exam periods  
- **Evidence grade**: B for fatigue/stress; C as general enhancer  
- **Typical dose**: 200–600 mg standardized extract, morning  
- **Onset**: Acute to a few weeks  
- **Safety**: Generally well tolerated; can be mildly stimulating. Theoretical mild MAO inhibition — caution with MAOIs.  
- **Notes**: Best with an identifiable stressor or fatigue.

### Ashwagandha
- **Status**: OTC  
- **Primary outcome domains**: Stress Resilience / Mood  
- **Mechanism tags**: Adaptogen, Cortisol / HPA modulator  
- **Best evidence contexts**: Stressed / anxious adults  
- **Evidence grade**: B for stress/anxiety; C for pure cognition  
- **Typical dose**: 300–600 mg standardized root extract (e.g., KSM-66)  
- **Onset**: Weeks  
- **Safety**: Generally good; rare liver-injury reports.  

### Lion’s Mane
- **Status**: OTC  
- **Primary outcome domains**: Memory  
- **Mechanism tags**: (limited clear mechanism tags)  
- **Best evidence contexts**: Older adults with mild cognitive impairment  
- **Evidence grade**: C  
- **Typical dose**: 1–3 g fruiting body or extracts  
- **Onset**: Weeks  
- **Safety**: Well tolerated.  

### Ginkgo biloba
- **Status**: OTC  
- **Primary outcome domains**: Memory  
- **Mechanism tags**: (circulation-related; limited)  
- **Best evidence contexts**: Cognitive impairment / older adults  
- **Evidence grade**: C in healthy; mixed in impaired  
- **Typical dose**: 120–240 mg/day standardized EGb 761 extract (divided doses)  
- **Safety**: Caution with anticoagulants/antiplatelets (case reports of bleeding; controlled data mixed/negative for EGb 761).  

### Creatine monohydrate
- **Status**: OTC  
- **Primary outcome domains**: Memory, Mental Energy / Fatigue Resistance, Physical Performance / Recovery  
- **Mechanism tags**: Mitochondrial / Energy metabolism  
- **Best evidence contexts**: Older adults, sleep deprivation / high demand; vegetarians  
- **Evidence grade**: B (memory, especially older/stressed)  
- **Typical dose**: 3–5 g/day  
- **Onset**: Days to weeks  
- **Key links**: Xu et al. 2024 meta [PMID: 39070254](https://pubmed.ncbi.nlm.nih.gov/39070254/); Prokopidis et al. 2023 memory meta [PMID: 35984306](https://pubmed.ncbi.nlm.nih.gov/35984306/)  
- **Safety**: Excellent long-term data.  
- **Notes**: Strongest signal when brain energy is challenged.

### Omega-3 (DHA-rich)
- **Status**: OTC  
- **Primary outcome domains**: Foundational / Nutrient Status, Memory  
- **Mechanism tags**: Membrane / Phospholipid support  
- **Best evidence contexts**: Deficiency states  
- **Evidence grade**: B for general brain health  
- **Typical dose**: 1–2 g/day combined EPA + DHA (common studied range for cognitive/brain health endpoints)  
- **Safety**: Generally excellent.

### B-vitamins (especially B12, folate)
- **Status**: OTC  
- **Primary outcome domains**: Foundational / Nutrient Status  
- **Mechanism tags**: (nutrient repletion)  
- **Best evidence contexts**: Deficiency  
- **Evidence grade**: A for correcting deficiency; D as enhancer when replete  
- **Typical dose**: Repletion dosing varies by deficiency severity and form (e.g., B12 500–1000 µg/day oral or as directed; folate 400–800 µg); higher only under clinical guidance for deficiency.  

### Methylphenidate
- **Status**: **PRESCRIPTION**  
- **Primary outcome domains**: Attention / Focus  
- **Mechanism tags**: Stimulant  
- **Best evidence contexts**: ADHD diagnosis (first-line)  
- **Evidence grade**: A for ADHD  
- **Safety**: Requires medical supervision.

### Amphetamine-based (Adderall, Vyvanse, etc.)
- **Status**: **PRESCRIPTION**  
- **Primary outcome domains**: Attention / Focus  
- **Mechanism tags**: Stimulant  
- **Best evidence contexts**: ADHD diagnosis  
- **Evidence grade**: A for ADHD  
- **Safety**: Same caveats as methylphenidate.

### Modafinil / Armodafinil
- **Status**: **PRESCRIPTION**  
- **Primary outcome domains**: Mental Energy / Fatigue Resistance, Attention / Focus  
- **Mechanism tags**: Wakefulness agent  
- **Best evidence contexts**: Narcolepsy, shift-work, sleep deprivation  
- **Evidence grade**: A for approved indications; C as general nootropic in rested healthy people  
- **Safety**: Requires prescription.

---

## 6. Peptides & Investigational Compounds

### Retatrutide (LY3437943)
- **Status**: Investigational (Phase 3; not approved)  
- **Primary outcome domains**: Physical Performance / Recovery (body composition), Foundational / Nutrient Status (glycemic/metabolic)  
- **Mechanism tags**: Triple GLP-1 / GIP / glucagon receptor agonist  
- **Best evidence contexts**: Obesity, type 2 diabetes, metabolic disease (weight loss and glycemic control)  
- **Evidence grade**: A for metabolic outcomes in Phase 2/3 trials; D for direct cognitive enhancement in healthy adults  
- **Typical dose**: Studied in clinical trials (not for self-administration)  
- **Onset**: Weeks  
- **Key notes**: Strong Phase 2 weight-loss data (up to ~24%+ at 48 weeks). Preclinical diabetic rat data show partial attenuation of diabetes-associated learning/memory deficits (e.g., Morris Water Maze) and reduced hippocampal inflammation; no human cognitive enhancement data in healthy subjects. GI side effects common (nausea, etc.).  
- **Safety**: Requires clinical supervision. Not available by prescription. Research-use only in grey market carries purity and dosing risks.

### Semax
- **Status**: Research / approved in some jurisdictions (Russia); not FDA-approved  
- **Primary outcome domains**: Attention / Focus, Memory  
- **Mechanism tags**: ACTH fragment analog (BDNF modulation)  
- **Best evidence contexts**: Neurological recovery (stroke, cerebrovascular); limited for healthy adults  
- **Evidence grade**: C (mostly Russian clinical data in patient populations; weak independent RCTs in healthy people)  
- **Typical dose**: Russian clinical use (intranasal) often 200–600 µg/day. Community/research chemical reports commonly cite 200–600 µg/day intranasal (sometimes split). No established dose for healthy adults.  
- **Community sources**: Reddit r/Nootropics, r/Peptides threads on dosing (e.g., discussions of 300–600 mcg range).  
- **Onset**: Acute to days  
- **Notes**: Most human data from Russian groups in clinical populations. No strong placebo-controlled evidence of cognitive benefit in healthy adults. Research chemical outside approved jurisdictions. Not medical advice.

### Selank
- **Status**: Research / approved in some jurisdictions (Russia); not FDA-approved  
- **Primary outcome domains**: Stress Resilience / Mood, Attention / Focus  
- **Mechanism tags**: Tuftsin analog (GABAergic / anxiolytic)  
- **Best evidence contexts**: Anxiety disorders (limited controlled data)  
- **Evidence grade**: C (small Russian trials, often without placebo; weak for healthy cognition)  
- **Typical dose**: Russian clinical use (intranasal) often ~250–500 µg/day (up to higher divided). Community reports commonly 250–500 µg/day intranasal.  
- **Community sources**: Reddit r/Nootropics and r/Peptides dosing discussions.  
- **Notes**: Anxiolytic signals in limited studies; not established as a nootropic for healthy users. Research chemical outside approved jurisdictions. Not medical advice.

### BPC-157
- **Status**: Research (not approved)  
- **Primary outcome domains**: Physical Performance / Recovery  
- **Mechanism tags**: Synthetic gastric peptide fragment (angiogenesis / repair pathways)  
- **Best evidence contexts**: Animal models of tendon, ligament, gut, and some CNS injury  
- **Evidence grade**: C for recovery (strong animal data); D for human efficacy (few uncontrolled reports)  
- **Typical dose**: No established human dose. Most common community-reported protocol: 250–500 mcg/day (often 250 mcg 1–2× daily subcutaneous). Oral also reported at similar ranges.  
- **Community sources**: Reddit r/Peptides, r/bpc_157, r/Biohackers (multiple threads summarizing 250–500 mcg range). Summaries at sites aggregating Reddit reports.  
- **Notes**: Large rodent literature; human data limited to small uncontrolled reports. Not a primary cognitive agent. Research chemical only. Purity varies widely. Not medical advice.

### TB-500 (Thymosin beta-4 fragment)
- **Status**: Research (not approved)  
- **Primary outcome domains**: Physical Performance / Recovery  
- **Mechanism tags**: Actin-binding fragment (cell migration / repair)  
- **Best evidence contexts**: Animal wound/repair models; full-length protein has limited human trial data  
- **Evidence grade**: C/D (preclinical for fragment; human data sparse or on parent molecule)  
- **Typical dose**: No established human dose. Common community protocol: loading 2–2.5 mg 2×/week for 4–6 weeks, then lower maintenance (e.g., 2 mg weekly).  
- **Community sources**: Reddit r/Peptides and related biohacking forums; multiple protocol summaries citing the loading/maintenance pattern.  
- **Notes**: Often stacked with BPC-157 in grey market; combination has no controlled human data. Research chemical only. Purity varies. Not medical advice.

### GLOW blend (GHK-Cu + BPC-157 + TB-500)
- **Status**: Research chemical blend (not approved; no studies of the blend)  
- **Primary outcome domains**: Physical Performance / Recovery, (skin / tissue repair – community interest)  
- **Mechanism tags**: Copper peptide + repair peptides  
- **Best evidence contexts**: None for the blend. Components: GHK-Cu has mixed topical human cosmetic data; BPC-157 and TB-500 mostly animal repair models.  
- **Evidence grade**: D for the blend (no human or controlled combination data)  
- **Typical dose**: No established dose. Common vial is ~50/10/10 mg (GHK-Cu/BPC/TB). Community reports often use 10–20 units daily (reconstituted, delivering ~250–500 mcg each of BPC/TB plus higher GHK-Cu).  
- **Community sources**: Reddit r/Peptides and biohacking discussions of GLOW blends; protocol aggregators.  
- **Notes**: Popular community stack. Marketed for skin, hair, recovery. Anecdotal reports of skin reactions then improvement. Purity and labeling vary widely. No blend-level evidence. Research chemical only. Not medical advice.

### KLOW blend (GLOW + KPV)
- **Status**: Research chemical blend (not approved; no studies of the blend)  
- **Primary outcome domains**: Physical Performance / Recovery, Stress Resilience / Mood (inflammation modulation – community)  
- **Mechanism tags**: Copper peptide + repair peptides + α-MSH fragment (KPV)  
- **Best evidence contexts**: None for the blend. KPV has preclinical anti-inflammatory / gut data (e.g., NF-κB, colitis models).  
- **Evidence grade**: D for the blend  
- **Typical dose**: No established dose. Common vial ~50/10/10/10 mg. Community protocols often mirror GLOW with added KPV (similar unit draws delivering ~200–500 mcg of each small peptide).  
- **Community sources**: Reddit and biohacking discussions comparing GLOW vs KLOW.  
- **Notes**: GLOW plus KPV. Community preference when inflammation or gut is a focus. Same purity and lack-of-blend-data caveats. Research chemical only. Not medical advice.

### Phosphatidylserine (PS)
- **Status**: OTC  
- **Primary outcome domains**: Memory, Stress Resilience / Mood  
- **Mechanism tags**: Membrane / Phospholipid support  
- **Best evidence contexts**: Older adults with age-related memory complaints or mild cognitive impairment; some data on exercise stress cortisol  
- **Evidence grade**: B (modest memory benefits in older/impaired; mixed in other populations)  
- **Typical dose**: 100–300 mg/day  
- **Onset**: Weeks  
- **Key notes**: Multiple RCTs and meta-analyses support modest cognitive gains, especially memory, in elderly with decline. Source and formulation vary across studies. Limited long-term data.  
- **Safety**: Generally well tolerated.

### Acetyl-L-Carnitine (ALCAR)
- **Status**: OTC  
- **Primary outcome domains**: Memory, Mental Energy / Fatigue Resistance, Stress Resilience / Mood  
- **Mechanism tags**: Mitochondrial / Energy metabolism, Amino acid / Neurotransmitter precursor  
- **Best evidence contexts**: Older adults with mild cognitive impairment (MCI) or early Alzheimer's; elderly with fatigue; depressive symptoms (especially older adults)  
- **Evidence grade**: B (modest cognitive benefit in MCI/early AD meta-analyses; strong signal for depression symptoms)  
- **Typical dose**: 1.5–3 g/day  
- **Onset**: Weeks to months  
- **Key links**: Montgomery et al. 2003 meta [PMID: 12598816](https://pubmed.ncbi.nlm.nih.gov/12598816/); depression meta [PMID: 29076953](https://pubmed.ncbi.nlm.nih.gov/29076953/)  
- **Safety**: Generally well tolerated.  
- **Notes**: Strongest signals in populations with impairment or age-related decline rather than healthy young adults.

### Huperzine A
- **Status**: OTC (supplement form; regulated differently by jurisdiction)  
- **Primary outcome domains**: Memory, Attention / Focus  
- **Mechanism tags**: Cholinergic (acetylcholinesterase inhibitor)  
- **Best evidence contexts**: Alzheimer's disease, mild cognitive impairment (mostly Chinese RCTs and meta-analyses)  
- **Evidence grade**: B for cognitive scores in AD/MCI (heterogeneous quality, mostly non-Western trials); C for healthy adults  
- **Typical dose**: 100–400 µg/day  
- **Onset**: Weeks  
- **Key notes**: Multiple meta-analyses (e.g., Yang et al. on AD/VD) show improvements in MMSE and ADL in dementia populations. Mild side effects (GI, dizziness). Limited high-quality independent Western RCTs.  
- **Safety**: Generally mild side effects; caution with other cholinergics or in certain conditions.

### Noopept (Omberacetam)
- **Status**: Research / prescription in some jurisdictions (Russia); not widely approved as supplement  
- **Primary outcome domains**: Memory, Attention / Focus  
- **Mechanism tags**: Peptide-like (proline dipeptide; BDNF/NGF modulation in preclinical)  
- **Best evidence contexts**: Mild cognitive impairment from vascular or traumatic causes (small Russian open-label or comparative studies)  
- **Evidence grade**: C (limited human data, mostly small Russian trials without strong placebo controls; no robust data in healthy adults)  
- **Typical dose**: 10–30 mg/day (studied ~20 mg)  
- **Onset**: Days to weeks  
- **Key notes**: Comparative studies vs piracetam in impaired populations report cognitive and anxiolytic signals. No high-quality RCTs in healthy users. Preclinical BDNF data.  
- **Safety**: Limited long-term data; generally reported as tolerable in short studies.

---

## 7. Longevity / Healthspan Compounds

### Rapamycin (Sirolimus)
- **Status**: **PRESCRIPTION** (approved for transplant/immunosuppression; used off-label for longevity)  
- **Primary outcome domains**: Longevity / Healthspan  
- **Mechanism tags**: mTORC1 inhibitor  
- **Best evidence contexts**: Animal lifespan extension (strongest pharmacological signal in ITP mice); human intermittent low-dose safety and surrogate markers  
- **Evidence grade**: A in animal models for lifespan; C for human healthspan/longevity (no lifespan trials; PEARL shows safety of weekly low-dose with modest signals in lean mass/pain for women)  
- **Typical dose**: Intermittent (e.g., 5–10 mg weekly studied in PEARL)  
- **Key notes**: Strongest and most replicated mammalian lifespan data of any drug. PEARL trial (48 weeks, healthy older adults) found acceptable safety, no change in primary visceral fat endpoint, lean tissue and pain improvements in women on higher dose. No human all-cause mortality or lifespan data.  
- **Safety**: Immunosuppressive at higher/continuous doses; intermittent low-dose appears better tolerated in short-term trials. Requires medical supervision. Off-label use.

### Metformin
- **Status**: **PRESCRIPTION** (approved for type 2 diabetes; used off-label for longevity research)  
- **Primary outcome domains**: Longevity / Healthspan, Foundational / Nutrient Status (glycemic)  
- **Mechanism tags**: AMPK activator  
- **Best evidence contexts**: Type 2 diabetes (strong); observational signals in diabetics for survival; limited for non-diabetics  
- **Evidence grade**: A for diabetes outcomes; C for longevity in healthy/non-diabetic adults (observational only; TAME trial designed but no published efficacy results as of late 2026)  
- **Typical dose**: Standard diabetes dosing (e.g., 500–2000 mg/day)  
- **Key notes**: Some observational data suggested diabetics on metformin outlived non-diabetic controls, but replication mixed and confounded by disease. Can blunt exercise adaptations in some studies. TAME (composite aging endpoint in non-diabetics) has faced funding delays and has not delivered results.  
- **Safety**: Well-characterized for diabetes use; GI side effects common; monitor for B12, kidney function. Off-label longevity use requires clinician oversight.

### NAD+ precursors (NMN / NR)
- **Status**: OTC (supplements)  
- **Primary outcome domains**: Longevity / Healthspan, Foundational / Nutrient Status  
- **Mechanism tags**: NAD+ precursor  
- **Best evidence contexts**: Biomarker (NAD+ levels); mixed functional outcomes in small trials  
- **Evidence grade**: C (raises NAD+; clinical/functional benefits inconsistent or small across trials)  
- **Typical dose**: NMN commonly studied 250–1000 mg/day; NR commonly studied 100–1000 mg/day (higher short-term doses also tested). Most trials short-term.  
- **Notes**: Multiple small RCTs. No mammalian lifespan extension data comparable to rapamycin. Emerging.

---

## Additional High-Interest Compounds Across Domains

### Magnesium (general + L-threonate)
- **Status**: OTC  
- **Primary outcome domains**: Sleep Architecture, Stress Resilience / Mood, Foundational / Nutrient Status, Memory (L-threonate)  
- **Mechanism tags**: Mineral cofactor, NMDA modulation (L-threonate)  
- **Best evidence contexts**: Deficiency states; sleep/anxiety support; limited cognitive data for L-threonate  
- **Evidence grade**: A for correcting deficiency and general magnesium benefits; C for L-threonate-specific cognitive claims (small, often industry-funded trials)  
- **Typical dose**: General: 200–400 mg elemental/day (glycinate or citrate common). L-threonate: 1–2 g/day of the salt (≈75–145 mg elemental) in studied trials.  
- **Safety**: Excellent at repletion doses. GI upset with some forms.  
- **Notes**: L-threonate has small human trials showing modest cognitive composite improvements; brain magnesium elevation primarily shown in animals.

### Vitamin D3
- **Status**: OTC  
- **Primary outcome domains**: Foundational / Nutrient Status, Longevity / Healthspan, Physical Performance / Recovery  
- **Mechanism tags**: Hormone precursor / immune modulation  
- **Best evidence contexts**: Deficiency states (very common)  
- **Evidence grade**: A for correcting deficiency and associated outcomes; lower as general enhancer when replete  
- **Typical dose**: 1000–4000 IU/day common; dose to blood level (target often 30–50 ng/mL) under testing.  
- **Safety**: Excellent within studied ranges; toxicity rare below 10,000 IU/day chronically in most.  
- **Notes**: One of the most studied foundational supplements. Test levels before high dosing.

### CoQ10 / Ubiquinol
- **Status**: OTC  
- **Primary outcome domains**: Physical Performance / Recovery, Longevity / Healthspan, Mental Energy / Fatigue Resistance  
- **Mechanism tags**: Mitochondrial / Energy metabolism  
- **Best evidence contexts**: Statin users (muscle symptoms), heart failure, mitochondrial support  
- **Evidence grade**: B for statin-associated muscle symptoms and some cardiovascular contexts; C as general nootropic or longevity agent  
- **Typical dose**: 100–300 mg/day (ubiquinol form often preferred for bioavailability).  
- **Safety**: Excellent.  
- **Notes**: Endogenous production declines with age and statin use.

### Berberine
- **Status**: OTC  
- **Primary outcome domains**: Foundational / Nutrient Status (metabolic), Longevity / Healthspan  
- **Mechanism tags**: AMPK activation / metabolic  
- **Best evidence contexts**: Metabolic syndrome, type 2 diabetes support, lipid management  
- **Evidence grade**: B for glycemic and lipid effects (multiple RCTs/meta); C for pure longevity or cognitive claims  
- **Typical dose**: 500 mg 2–3×/day with meals.  
- **Safety**: GI upset common. Potential interactions with CYP substrates and other glucose-lowering agents.  
- **Notes**: Strongest human data among many “longevity” botanicals for metabolic endpoints.

### Urolithin A
- **Status**: OTC  
- **Primary outcome domains**: Physical Performance / Recovery, Longevity / Healthspan  
- **Mechanism tags**: Mitophagy / mitochondrial  
- **Best evidence contexts**: Older adults (muscle endurance)  
- **Evidence grade**: C (small company-funded RCTs show signals on muscle endurance and some biomarkers; limited independent replication)  
- **Typical dose**: 500–1000 mg/day in trials.  
- **Safety**: Well tolerated in available trials.  
- **Notes**: One of the better-studied newer longevity compounds for functional muscle outcomes in older adults.

### Glycine
- **Status**: OTC  
- **Primary outcome domains**: Sleep Architecture, Stress Resilience / Mood  
- **Mechanism tags**: Amino acid / inhibitory neurotransmitter  
- **Best evidence contexts**: Sleep quality, body temperature regulation  
- **Evidence grade**: B for sleep quality (several small RCTs)  
- **Typical dose**: 3 g before bed.  
- **Safety**: Excellent.  
- **Notes**: Often paired with magnesium for sleep stacks.

### Melatonin
- **Status**: OTC  
- **Primary outcome domains**: Sleep Architecture  
- **Mechanism tags**: Circadian / hormone  
- **Best evidence contexts**: Circadian disruption, delayed sleep phase, jet lag  
- **Evidence grade**: B for sleep onset in appropriate contexts; lower as general enhancer  
- **Typical dose**: 0.5–5 mg (lower doses often sufficient; timing more important than dose).  
- **Safety**: Generally good short-term. Daytime grogginess if mistimed or high dose.  
- **Notes**: Best used for timing issues rather than as a high-dose sedative.

### CJC-1295 + Ipamorelin (stack)
- **Status**: Research (not approved; compounding concerns noted by FDA)  
- **Primary outcome domains**: Physical Performance / Recovery, Longevity / Healthspan  
- **Mechanism tags**: GHRH analog + ghrelin receptor agonist (GH axis)  
- **Best evidence contexts**: Pharmacodynamic GH/IGF-1 elevation (individual compounds)  
- **Evidence grade**: C for GH elevation (small studies on individuals); D for body composition, recovery, or longevity outcomes (no controlled combination trials)  
- **Typical dose**: No established human dose for the blend. Community reports commonly 100–300 mcg of each, 1–2× daily (often bedtime).  
- **Community sources**: Reddit r/Peptides and bodybuilding forums.  
- **Notes**: Individual compounds raise GH/IGF-1 in small studies. Combination has no controlled efficacy data. Research chemical only. WADA prohibited. Not medical advice.

---

## Cardiovascular, Performance & Hormone-Modulation Compounds

### Nattokinase (Natto extract)
- **Status**: OTC  
- **Primary outcome domains**: Physical Performance / Recovery (vascular), Longevity / Healthspan  
- **Mechanism tags**: Fibrinolytic enzyme  
- **Best evidence contexts**: Mild blood pressure reduction; mixed lipid and atherosclerosis data  
- **Evidence grade**: B for modest BP reduction (meta of RCTs); C for lipids/atherosclerosis progression (mixed results, one large retrospective positive at high dose, negative long-term RCT at standard dose)  
- **Typical dose**: 2,000–4,000 FU/day (most trials ~2,000 FU). Higher doses (up to 10,800 FU) reported in some observational work.  
- **Safety**: Caution with anticoagulants/antiplatelets — increased bleeding risk. Avoid with active bleeding disorders.  
- **Notes**: Fibrinolytic activity is real. Clinical event reduction not established. Not a substitute for prescribed anticoagulants.

### DIM (Diindolylmethane)
- **Status**: OTC  
- **Primary outcome domains**: Foundational / Nutrient Status (hormone metabolism)  
- **Mechanism tags**: Estrogen metabolism modulator (AhR / CYP pathways)  
- **Best evidence contexts**: Shift toward 2-hydroxy estrogen metabolites; limited clinical outcome data  
- **Evidence grade**: C (consistent biochemical metabolite shifts in small trials; clinical endpoints like cancer risk or symptoms preliminary)  
- **Typical dose**: 100–200 mg/day (up to 300 mg studied). Take with food.  
- **Safety**: Generally well tolerated. Potential interactions with tamoxifen (metabolite levels). Caution in hormone-sensitive conditions without clinical guidance.  
- **Notes**: Derived from cruciferous vegetables. Mainly shifts metabolite ratios rather than lowering total estrogen.

### Betaine Anhydrous (TMG)
- **Status**: OTC  
- **Primary outcome domains**: Physical Performance / Recovery, Foundational / Nutrient Status (homocysteine)  
- **Mechanism tags**: Methyl donor / osmolyte  
- **Best evidence contexts**: Homocysteine reduction; mixed performance/body composition signals  
- **Evidence grade**: B for lowering homocysteine (consistent RCTs); C for strength/power/lean mass (small positive trials, mixed overall)  
- **Typical dose**: 2.5 g/day for performance studies; 1.5–6 g/day for homocysteine.  
- **Safety**: Higher doses (>4–6 g) may raise LDL/total cholesterol in some.  
- **Notes**: Distinct from betaine HCl. Useful methyl donor pathway independent of folate/B12.

### Beta-Alanine
- **Status**: OTC  
- **Primary outcome domains**: Physical Performance / Recovery  
- **Mechanism tags**: Carnosine precursor / intramuscular buffer  
- **Best evidence contexts**: High-intensity efforts lasting ~1–10 minutes  
- **Evidence grade**: B (ISSN position stand; multiple meta-analyses show small but reliable benefits in appropriate exercise durations)  
- **Typical dose**: 3.2–6.4 g/day (split doses to minimize paresthesia). Loading over 2–4+ weeks.  
- **Safety**: Harmless tingling (paresthesia) at single high doses; mitigated by splitting or sustained-release.  
- **Notes**: Builds muscle carnosine over weeks. Most useful for glycolytic efforts.

### Beetroot (nitrate) / Beetroot powder
- **Status**: OTC / food  
- **Primary outcome domains**: Physical Performance / Recovery  
- **Mechanism tags**: Nitrate → nitric oxide  
- **Best evidence contexts**: Endurance/time-to-exhaustion in recreational athletes; modest BP effects  
- **Evidence grade**: B for small performance benefits in recreational users (meta-analyses); lower in elite endurance athletes  
- **Typical dose**: ~5–8 mmol nitrate (roughly 300–500 mg nitrate) 2–3 hours pre-exercise, or chronic multi-day use.  
- **Safety**: Excellent as food-derived. Avoid antiseptic mouthwash around dosing (disrupts oral bacteria needed for conversion).  
- **Notes**: Effect size small. Synergistic interest with citrulline in limited data.

### L-Citrulline
- **Status**: OTC  
- **Primary outcome domains**: Physical Performance / Recovery  
- **Mechanism tags**: Arginine precursor / nitric oxide support  
- **Best evidence contexts**: Resistance training volume / repetitions to failure; some recovery signals  
- **Evidence grade**: B/C (meta-analyses show small benefits on strength/power and reps to failure; mixed on endurance)  
- **Typical dose**: ≥3 g/day (often 6–8 g citrulline malate). Acute or chronic.  
- **Safety**: Excellent.  
- **Notes**: Better oral bioavailability than L-arginine for raising plasma arginine.

### Hibiscus tea / extract
- **Status**: OTC / food  
- **Primary outcome domains**: Physical Performance / Recovery (vascular), Foundational / Nutrient Status  
- **Mechanism tags**: Polyphenols / mild diuretic / antioxidant  
- **Best evidence contexts**: Mild blood pressure reduction  
- **Evidence grade**: B for modest BP lowering (several RCTs/meta)  
- **Typical dose**: 1–3 cups tea or equivalent extract daily.  
- **Safety**: Generally safe. Potential additive effects with antihypertensives.  
- **Notes**: Common traditional use with supporting human BP data.

### Naringin
- **Status**: OTC  
- **Primary outcome domains**: Foundational / Nutrient Status, Longevity / Healthspan  
- **Mechanism tags**: Citrus flavonoid / CYP3A4 modulator  
- **Best evidence contexts**: Preclinical metabolic and vascular; limited human outcome data  
- **Evidence grade**: C/D (mostly animal and in vitro; human trials sparse for clinical endpoints)  
- **Typical dose**: Variable in supplements (often 100–500 mg); food source is grapefruit.  
- **Safety**: Can inhibit CYP3A4 — significant drug interaction potential (similar caution to grapefruit).  
- **Notes**: Grapefruit-related flavonoid. Interaction risk is the main practical note. Limited high-quality human efficacy data for biohacking claims.

---

## Top Missing Compounds — Round Expansion

### N-Acetyl Cysteine (NAC)
- **Status**: OTC  
- **Primary outcome domains**: Foundational / Nutrient Status, Stress Resilience / Mood, Longevity / Healthspan  
- **Mechanism tags**: Glutathione precursor / antioxidant  
- **Best evidence contexts**: Mucolytic use, psychiatric adjunct (some), oxidative stress support  
- **Evidence grade**: B for certain clinical uses (e.g., acetaminophen toxicity medical, COPD mucolytic); C for general cognitive or longevity claims  
- **Typical dose**: 600–1800 mg/day.  
- **Safety**: Generally good; GI upset possible. Caution with nitroglycerin.  
- **Notes**: Strong glutathione support. Widely used in biohacking stacks.

### Taurine
- **Status**: OTC  
- **Primary outcome domains**: Physical Performance / Recovery, Longevity / Healthspan, Mental Energy / Fatigue Resistance  
- **Mechanism tags**: Amino acid / osmolyte / mitochondrial  
- **Best evidence contexts**: Exercise performance, blood pressure, mixed metabolic  
- **Evidence grade**: B for modest performance and BP effects; C for pure longevity (strong animal data, human age-decline claim contested)  
- **Typical dose**: 1–3 g/day.  
- **Safety**: Excellent.  
- **Notes**: Common in energy drinks. Human longevity data still emerging/mixed.

### Spermidine
- **Status**: OTC  
- **Primary outcome domains**: Longevity / Healthspan, Memory  
- **Mechanism tags**: Autophagy induction  
- **Best evidence contexts**: Preclinical lifespan; limited human cognitive/biomarker  
- **Evidence grade**: C (one major memory trial null overall; some secondary signals; strong animal data)  
- **Typical dose**: 1–5 mg/day in supplements (higher in food sources like wheat germ).  
- **Safety**: Well tolerated in available trials.  
- **Notes**: Popular longevity compound. Human functional data still limited.

### Fisetin
- **Status**: OTC  
- **Primary outcome domains**: Longevity / Healthspan  
- **Mechanism tags**: Senolytic (proposed) / flavonoid  
- **Best evidence contexts**: Preclinical senolytic; no robust human senolytic RCTs  
- **Evidence grade**: D for human senolytic claims (mostly animal); C as general flavonoid  
- **Typical dose**: 100–500 mg intermittent or daily in community use.  
- **Safety**: Limited long-term human data.  
- **Notes**: Often stacked with quercetin for senolytic protocols. Human evidence thin.

### Quercetin
- **Status**: OTC  
- **Primary outcome domains**: Longevity / Healthspan, Foundational / Nutrient Status  
- **Mechanism tags**: Flavonoid / senolytic (with dasatinib), antioxidant  
- **Best evidence contexts**: Allergies, some metabolic; senolytic mostly with dasatinib  
- **Evidence grade**: C (multiple small trials; senolytic effect primarily in combination pilot)  
- **Typical dose**: 500–1000 mg/day.  
- **Safety**: Generally good. May interact with some drugs.  
- **Notes**: Common in senolytic stacks. Standalone human data modest.

### Curcumin (Turmeric)
- **Status**: OTC  
- **Primary outcome domains**: Foundational / Nutrient Status, Stress Resilience / Mood, Longevity / Healthspan  
- **Mechanism tags**: NF-κB / anti-inflammatory  
- **Best evidence contexts**: Joint pain, some mood and metabolic markers  
- **Evidence grade**: B for certain inflammatory/pain contexts (meta-analyses); lower for pure cognitive  
- **Typical dose**: 500–2000 mg/day of curcuminoids (bioavailability-enhanced forms preferred).  
- **Safety**: Excellent. High doses can cause GI upset.  
- **Notes**: Bioavailability is the main practical issue; piperine or specialized formulations used.

### Sulforaphane
- **Status**: OTC (broccoli sprout extracts)  
- **Primary outcome domains**: Longevity / Healthspan, Foundational / Nutrient Status  
- **Mechanism tags**: Nrf2 activator / detoxification  
- **Best evidence contexts**: Detoxification markers, some cancer-risk biomarker studies  
- **Evidence grade**: B for Nrf2 activation and certain biomarker shifts; C for clinical outcomes  
- **Typical dose**: Variable (myrosinase-active products preferred); often equivalent to sprout extracts.  
- **Safety**: Excellent.  
- **Notes**: Best from active myrosinase-containing preparations. Strong mechanistic interest.

### Apigenin
- **Status**: OTC  
- **Primary outcome domains**: Sleep Architecture, Stress Resilience / Mood  
- **Mechanism tags**: Flavonoid / GABA modulation (preclinical)  
- **Best evidence contexts**: Chamomile extract (mixed sleep data); isolated apigenin human data sparse  
- **Evidence grade**: C (chamomile trials mixed; no strong isolated-apigenin sleep RCTs)  
- **Typical dose**: 50 mg before bed commonly used in community protocols.  
- **Safety**: Good.  
- **Notes**: Popular in sleep stacks (often with magnesium + theanine). Human evidence for isolated form is limited.

### Tongkat Ali (Eurycoma longifolia)
- **Status**: OTC  
- **Primary outcome domains**: Physical Performance / Recovery, Stress Resilience / Mood  
- **Mechanism tags**: Adaptogen / testosterone support (modest)  
- **Best evidence contexts**: Men with low/borderline testosterone or high stress  
- **Evidence grade**: C (meta shows modest testosterone rise, strongest in hypogonadal; mixed in healthy men)  
- **Typical dose**: 200–400 mg/day standardized extract.  
- **Safety**: Generally good short-term. Quality varies.  
- **Notes**: Clearest signal in stressed or low-T men. Not a strong anabolic for healthy young men.

### Collagen Peptides
- **Status**: OTC  
- **Primary outcome domains**: Physical Performance / Recovery (joints/skin)  
- **Mechanism tags**: Amino acid source / connective tissue support  
- **Best evidence contexts**: Skin hydration/elasticity (mixed, industry-heavy); joint pain in OA (modest)  
- **Evidence grade**: C (positive pooled results for skin and joints, but high-quality/independent trials weaker; funding bias noted)  
- **Typical dose**: 2.5–10 g/day hydrolyzed collagen.  
- **Safety**: Excellent.  
- **Notes**: Practical amino acid source. Benefits more consistent for skin markers than dramatic joint repair.

### Astaxanthin
- **Status**: OTC  
- **Primary outcome domains**: Physical Performance / Recovery, Longevity / Healthspan  
- **Mechanism tags**: Carotenoid antioxidant  
- **Best evidence contexts**: Skin, eye, exercise recovery markers  
- **Evidence grade**: C (several small RCTs; mixed functional outcomes)  
- **Typical dose**: 4–12 mg/day.  
- **Safety**: Excellent.  
- **Notes**: Potent antioxidant with some human biomarker support.

### Pterostilbene
- **Status**: OTC  
- **Primary outcome domains**: Longevity / Healthspan  
- **Mechanism tags**: Stilbene / SIRT1 (related to resveratrol)  
- **Best evidence contexts**: Preclinical; limited human  
- **Evidence grade**: C/D (better bioavailability than resveratrol in some data; human outcomes sparse)  
- **Typical dose**: 50–250 mg/day.  
- **Safety**: Limited long-term data.  
- **Notes**: Resveratrol analog with claimed better absorption.

### Resveratrol
- **Status**: OTC  
- **Primary outcome domains**: Longevity / Healthspan  
- **Mechanism tags**: Stilbene / SIRT1 activation (debated)  
- **Best evidence contexts**: Biomarker and some metabolic; lifespan not shown in humans  
- **Evidence grade**: C (many small trials; bioavailability issues; no clear longevity outcome)  
- **Typical dose**: 150–500 mg/day.  
- **Safety**: Generally good; high doses can cause GI issues.  
- **Notes**: Classic longevity compound. Human evidence remains modest.

### Zinc
- **Status**: OTC  
- **Primary outcome domains**: Foundational / Nutrient Status, Physical Performance / Recovery  
- **Mechanism tags**: Mineral cofactor / immune  
- **Best evidence contexts**: Deficiency correction; immune support in deficiency  
- **Evidence grade**: A for deficiency; lower as enhancer when replete  
- **Typical dose**: 15–30 mg elemental/day (short-term higher for colds).  
- **Safety**: Excess can cause copper deficiency with long-term high doses.  
- **Notes**: Test status if considering high chronic doses.

### Vitamin K2
- **Status**: OTC  
- **Primary outcome domains**: Foundational / Nutrient Status, Longevity / Healthspan  
- **Mechanism tags**: Vitamin / calcium metabolism  
- **Best evidence contexts**: Bone and vascular calcification markers  
- **Evidence grade**: B for certain bone/vascular biomarker contexts; C for broad longevity  
- **Typical dose**: 100–200 mcg MK-7/day.  
- **Safety**: Excellent. Caution with warfarin.  
- **Notes**: Often paired with D3. Supportive data for directing calcium.

### Inositol
- **Status**: OTC  
- **Primary outcome domains**: Stress Resilience / Mood, Sleep Architecture  
- **Mechanism tags**: Sugar alcohol / second messenger  
- **Best evidence contexts**: Anxiety, PCOS, some sleep/mood  
- **Evidence grade**: B for certain anxiety and PCOS contexts  
- **Typical dose**: 2–18 g/day depending on indication (lower for general).  
- **Safety**: Excellent.  
- **Notes**: Myo-inositol is the common form studied.

### SAMe (S-Adenosylmethionine)
- **Status**: OTC  
- **Primary outcome domains**: Stress Resilience / Mood, Foundational / Nutrient Status  
- **Mechanism tags**: Methyl donor  
- **Best evidence contexts**: Depression adjunct, osteoarthritis  
- **Evidence grade**: B for depression and joint symptoms in some trials  
- **Typical dose**: 400–1600 mg/day.  
- **Safety**: Generally good. Potential mania trigger in bipolar.  
- **Notes**: Expensive. Enteric-coated forms preferred.

### 5-HTP
- **Status**: OTC  
- **Primary outcome domains**: Stress Resilience / Mood, Sleep Architecture  
- **Mechanism tags**: Serotonin precursor  
- **Best evidence contexts**: Mood, sleep onset (limited)  
- **Evidence grade**: C (small trials; mixed quality)  
- **Typical dose**: 50–300 mg/day.  
- **Safety**: Caution with SSRIs (serotonin syndrome risk).  
- **Notes**: Not for long-term unsupervised use with serotonergic drugs.

### Agmatine
- **Status**: OTC  
- **Primary outcome domains**: Physical Performance / Recovery, Stress Resilience / Mood  
- **Mechanism tags**: Nitric oxide / neuromodulation  
- **Best evidence contexts**: Preclinical pain/mood; limited human performance  
- **Evidence grade**: C/D (mostly animal and anecdotal human)  
- **Typical dose**: 1–2.5 g/day.  
- **Safety**: Limited human data.  
- **Notes**: Popular in some performance and recovery stacks.

### HMB (β-Hydroxy β-Methylbutyrate)
- **Status**: OTC  
- **Primary outcome domains**: Physical Performance / Recovery  
- **Mechanism tags**: Leucine metabolite / anti-catabolic  
- **Best evidence contexts**: Older adults, untrained, or high catabolic stress  
- **Evidence grade**: B for lean mass preservation in certain populations; lower in trained young athletes  
- **Typical dose**: 3 g/day.  
- **Safety**: Excellent.  
- **Notes**: More useful in recovery or older populations than advanced lifters.

### GlyNAC (Glycine + NAC)
- **Status**: OTC (combination)  
- **Primary outcome domains**: Longevity / Healthspan, Foundational / Nutrient Status  
- **Mechanism tags**: Glutathione support  
- **Best evidence contexts**: Older adults (small trials on biomarkers and some function)  
- **Evidence grade**: C (promising small human trials; limited independent replication)  
- **Typical dose**: Often 100 mg/kg glycine + 100 mg/kg NAC or fixed gram doses in studies.  
- **Safety**: Components are well-tolerated.  
- **Notes**: Emerging combination for glutathione and aging markers.

### Methylene Blue (low dose)
- **Status**: Research / OTC (low-dose formulations)  
- **Primary outcome domains**: Attention / Focus, Longevity / Healthspan  
- **Mechanism tags**: Mitochondrial / redox  
- **Best evidence contexts**: Preclinical cognitive; limited controlled human nootropic data  
- **Evidence grade**: C/D for cognitive enhancement (historical medical use; modern low-dose data thin)  
- **Typical dose**: Low-dose community use often 0.5–2 mg/kg or microdoses.  
- **Safety**: Can interact with SSRIs; pharmaceutical-grade only. Stains.  
- **Notes**: Research chemical at higher doses. Caution required.

### Epitalon (Epithalon)
- **Status**: Research  
- **Primary outcome domains**: Longevity / Healthspan, Sleep Architecture  
- **Mechanism tags**: Pineal peptide / telomerase (proposed)  
- **Best evidence contexts**: Russian clinical/observational; limited Western RCTs  
- **Evidence grade**: C/D (mostly one research group; biomarker and some lifespan-adjacent claims)  
- **Typical dose**: Community protocols vary widely (often short cycles).  
- **Safety**: Limited independent data.  
- **Notes**: Research chemical. Popular in longevity circles with thin Western evidence.

### GHK-Cu (standalone)
- **Status**: OTC / Research (injectable)  
- **Primary outcome domains**: Physical Performance / Recovery (skin/tissue)  
- **Mechanism tags**: Copper peptide  
- **Best evidence contexts**: Topical cosmetic (skin); injectable mostly anecdotal  
- **Evidence grade**: B for topical skin effects; D for injectable systemic claims  
- **Typical dose**: Topical varies; injectable community doses often 1–2 mg.  
- **Safety**: Topical generally good. Injectable purity concerns.  
- **Notes**: Stronger evidence as topical than injectable.

### KPV
- **Status**: Research  
- **Primary outcome domains**: Physical Performance / Recovery, Foundational / Nutrient Status  
- **Mechanism tags**: α-MSH fragment / anti-inflammatory  
- **Best evidence contexts**: Preclinical gut and inflammation models  
- **Evidence grade**: D for human (mostly animal)  
- **Typical dose**: No established human dose. Community reports often 200–500 mcg.  
- **Safety**: Limited data.  
- **Notes**: Component of KLOW. Research chemical only.

### Thymosin Alpha-1
- **Status**: Research / some jurisdictions approved for specific uses  
- **Primary outcome domains**: Foundational / Nutrient Status (immune)  
- **Mechanism tags**: Immune peptide  
- **Best evidence contexts**: Certain immunodeficiency or hepatitis contexts (limited)  
- **Evidence grade**: C (some clinical use data in specific conditions)  
- **Typical dose**: Medical protocols exist; community varies.  
- **Safety**: Generally well-tolerated in studied uses.  
- **Notes**: Immune-focused peptide. Not a general nootropic.

## Additional High-Interest Compounds (Round 2 Expansion)

### PQQ (Pyrroloquinoline Quinone)
- **Status**: OTC  
- **Primary outcome domains**: Longevity / Healthspan, Mental Energy / Fatigue Resistance, Physical Performance / Recovery  
- **Mechanism tags**: Mitochondrial biogenesis / antioxidant  
- **Best evidence contexts**: Cognitive function in older adults; some muscle/physical function signals  
- **Evidence grade**: B for cognitive endpoints in several RCTs; C for broader longevity  
- **Typical dose**: 20–21.5 mg/day (studied)  
- **Onset**: Weeks  
- **Key links**: Multiple RCTs including Shiojima 2022 cognitive battery [PMID: 34415830](https://pubmed.ncbi.nlm.nih.gov/34415830/)  
- **Safety**: Well-tolerated in trials up to 12 weeks.  
- **Notes**: One of the better-supported mitochondrial biogenesis compounds among supplements.

### R-Alpha-Lipoic Acid (R-ALA)
- **Status**: OTC  
- **Primary outcome domains**: Foundational / Nutrient Status, Longevity / Healthspan, Physical Performance / Recovery  
- **Mechanism tags**: Mitochondrial cofactor / antioxidant / AMPK  
- **Best evidence contexts**: Diabetic neuropathy (racemic ALA data); metabolic markers  
- **Evidence grade**: B for neuropathy symptoms (primarily racemic data); C for general use  
- **Typical dose**: 300–600 mg/day (R-form often lower)  
- **Safety**: Generally good. Can lower blood sugar.  
- **Notes**: R-enantiomer is the natural cofactor form. Most neuropathy data is on racemic mixture.

### Calcium Alpha-Ketoglutarate (Ca-AKG)
- **Status**: OTC  
- **Primary outcome domains**: Longevity / Healthspan  
- **Mechanism tags**: TCA intermediate / epigenetic (proposed)  
- **Best evidence contexts**: DNA methylation clock signals (mostly open-label/observational)  
- **Evidence grade**: C/D (promising animal data; human RCTs limited or ongoing)  
- **Typical dose**: ~1000 mg/day sustained-release Ca-AKG in studies/products  
- **Safety**: Limited long-term data.  
- **Notes**: Popular longevity compound. Stronger animal than human evidence currently. ABLE trial ongoing.

### Shilajit (purified extract)
- **Status**: OTC  
- **Primary outcome domains**: Physical Performance / Recovery, Foundational / Nutrient Status  
- **Mechanism tags**: Fulvic acid / mineral complex  
- **Best evidence contexts**: Testosterone in middle-aged men; some fatigue/strength preservation  
- **Evidence grade**: C (small RCTs, often single-sponsor)  
- **Typical dose**: 250–500 mg/day purified extract  
- **Safety**: Heavy-metal risk with untested products. Use third-party tested.  
- **Notes**: Composition varies by source. PrimaVie and similar standardized extracts dominate the literature.

### EGCG (Green Tea Extract)
- **Status**: OTC  
- **Primary outcome domains**: Longevity / Healthspan, Foundational / Nutrient Status  
- **Mechanism tags**: Polyphenol / antioxidant / metabolic  
- **Best evidence contexts**: Modest lipid effects; weight management signals  
- **Evidence grade**: B for some metabolic/lipid endpoints; C for longevity  
- **Typical dose**: Often 200–400 mg EGCG/day (stay well below 800 mg)  
- **Safety**: Liver enzyme elevation risk at ≥800 mg EGCG/day from supplements (EFSA). Prefer with food.  
- **Notes**: Brewed tea is safer. Concentrated extracts need caution.

### Lithium Orotate (low-dose)
- **Status**: OTC (low-dose)  
- **Primary outcome domains**: Stress Resilience / Mood, Longevity / Healthspan  
- **Mechanism tags**: GSK-3β inhibition / neuroprotective (proposed)  
- **Best evidence contexts**: Epidemiological (drinking water); one microdose MCI trial  
- **Evidence grade**: C (epidemiology + limited interventional)  
- **Typical dose**: 1–5 mg lithium orotate (delivering sub-mg elemental lithium)  
- **Safety**: Therapeutic psychiatric doses are much higher and require monitoring. Low-dose has limited safety data.  
- **Notes**: Not a substitute for prescribed lithium. Distinct from high-dose carbonate.

### Uridine Monophosphate (UMP) / Triacetyluridine (TAU)
- **Status**: OTC  
- **Primary outcome domains**: Memory, Stress Resilience / Mood  
- **Mechanism tags**: Membrane phospholipid precursor / CDP-choline pathway  
- **Best evidence contexts**: Multinutrient formulas (with DHA + choline) in MCI/prodromal AD; small open bipolar trial for TAU  
- **Evidence grade**: C (stronger as part of fixed multinutrient than standalone)  
- **Typical dose**: 150–500 mg UMP/day (often stacked with DHA and choline)  
- **Safety**: Generally well-tolerated. Avoid with certain chemotherapy.  
- **Notes**: Core of the “Mr. Happy Stack.” Standalone data thinner than stack data.

### MOTS-c
- **Status**: Research  
- **Primary outcome domains**: Longevity / Healthspan, Physical Performance / Recovery  
- **Mechanism tags**: Mitochondrial-derived peptide / AMPK  
- **Best evidence contexts**: Animal metabolic and exercise models; no completed human trials of the peptide itself  
- **Evidence grade**: D (preclinical only for exogenous administration)  
- **Typical dose**: No established human dose.  
- **Safety**: Unknown long-term. Product quality variable.  
- **Notes**: Research chemical. Endogenous levels rise with exercise. Analog CB4211 has limited human data.

### MK-677 (Ibutamoren)
- **Status**: Research  
- **Primary outcome domains**: Physical Performance / Recovery  
- **Mechanism tags**: Ghrelin mimetic / GH secretagogue  
- **Best evidence contexts**: Older adults (fat-free mass increase without strength gains)  
- **Evidence grade**: C (human trials exist but functional benefits limited; metabolic risks)  
- **Typical dose**: 10–25 mg/day in trials  
- **Safety**: Appetite increase, water retention, elevated fasting glucose, reduced insulin sensitivity. One trial stopped for heart-failure signal in frail elderly.  
- **Notes**: Oral. Not a peptide. Metabolic monitoring advisable. Research chemical.

### Dasatinib + Quercetin (D+Q senolytic)
- **Status**: Dasatinib = Prescription; Quercetin = OTC  
- **Primary outcome domains**: Longevity / Healthspan  
- **Mechanism tags**: Senolytic  
- **Best evidence contexts**: Small human trials showing reduced senescent cell markers; 2026 MASH fibrosis RCT  
- **Evidence grade**: C (early human signals; not approved for aging)  
- **Typical dose**: Intermittent (e.g., dasatinib 100 mg + quercetin 1000 mg, short cycles)  
- **Safety**: Dasatinib has serious risks (cytopenias, bleeding). Not for unsupervised use.  
- **Notes**: Prescription component. Quercetin alone is not equivalent. Experimental.

### Acarbose
- **Status**: Prescription  
- **Primary outcome domains**: Longevity / Healthspan, Foundational / Nutrient Status  
- **Mechanism tags**: Alpha-glucosidase inhibitor / metabolic  
- **Best evidence contexts**: Diabetes; ITP lifespan extension in mice (especially males)  
- **Evidence grade**: A for glycemic control; C/D for human longevity  
- **Typical dose**: Medical dosing for diabetes.  
- **Safety**: GI side effects common.  
- **Notes**: Approved diabetes drug. Longevity interest driven by animal data.

### SS-31 (Elamipretide)
- **Status**: Research / approved for Barth syndrome  
- **Primary outcome domains**: Longevity / Healthspan, Physical Performance / Recovery  
- **Mechanism tags**: Mitochondrial (cardiolipin)  
- **Best evidence contexts**: Barth syndrome (approved); other mitochondrial/cardiomyopathy trials mixed  
- **Evidence grade**: C for broader use (phase 3 trials often missed primary endpoints)  
- **Typical dose**: Investigational.  
- **Safety**: Studied in trials.  
- **Notes**: Mitochondrial-targeted peptide. Stronger in specific rare diseases than general longevity.

### Tesamorelin
- **Status**: Prescription  
- **Primary outcome domains**: Physical Performance / Recovery  
- **Mechanism tags**: GHRH analog  
- **Best evidence contexts**: HIV-associated lipodystrophy (approved indication)  
- **Evidence grade**: A for approved use; C for off-label longevity/body composition  
- **Typical dose**: 2 mg subcutaneous daily (approved)  
- **Safety**: IGF-1 elevation requires monitoring.  
- **Notes**: FDA-approved for specific indication. Stronger evidence base than most GH peptides.

### Sermorelin
- **Status**: Research / compounding  
- **Primary outcome domains**: Physical Performance / Recovery  
- **Mechanism tags**: GHRH analog  
- **Best evidence contexts**: Limited modern RCTs; historical use for GH deficiency  
- **Evidence grade**: C  
- **Typical dose**: Community and compounding protocols vary.  
- **Safety**: Generally milder than direct GH. IGF-1 monitoring recommended.  
- **Notes**: Shorter half-life than CJC-1295. Often stacked.

### AOD-9604
- **Status**: Research  
- **Primary outcome domains**: Physical Performance / Recovery  
- **Mechanism tags**: GH fragment (lipolytic, proposed)  
- **Best evidence contexts**: Preclinical fat loss; very limited human data  
- **Evidence grade**: D  
- **Typical dose**: No established human therapeutic dose.  
- **Safety**: Limited data.  
- **Notes**: Research chemical. Marketing exceeds evidence.

### FOXO4-DRI
- **Status**: Research  
- **Primary outcome domains**: Longevity / Healthspan  
- **Mechanism tags**: Senolytic (FOXO4-p53 disruption)  
- **Best evidence contexts**: Preclinical only  
- **Evidence grade**: D  
- **Typical dose**: None established.  
- **Safety**: Unknown.  
- **Notes**: Highly experimental senolytic peptide. No human data.

### Thymalin
- **Status**: Research  
- **Primary outcome domains**: Foundational / Nutrient Status (immune)  
- **Mechanism tags**: Thymic peptide bioregulator  
- **Best evidence contexts**: Russian clinical reports; limited Western independent data  
- **Evidence grade**: C/D  
- **Typical dose**: Short cycles in community protocols.  
- **Safety**: Limited independent data.  
- **Notes**: Distinct from Thymosin Alpha-1. Khavinson bioregulator lineage.

### Pinealon
- **Status**: Research  
- **Primary outcome domains**: Memory, Longevity / Healthspan  
- **Mechanism tags**: Brain bioregulator peptide  
- **Best evidence contexts**: Preclinical and developer reports  
- **Evidence grade**: D  
- **Typical dose**: None established in Western literature.  
- **Safety**: Unknown.  
- **Notes**: Khavinson peptide. Minimal independent human evidence.

### 5-Amino-1MQ
- **Status**: Research  
- **Primary outcome domains**: Physical Performance / Recovery, Longevity / Healthspan  
- **Mechanism tags**: NNMT inhibitor (proposed metabolic)  
- **Best evidence contexts**: Preclinical fat-cell and metabolic models  
- **Evidence grade**: D  
- **Typical dose**: Community reports only.  
- **Safety**: Unknown.  
- **Notes**: Research chemical. No meaningful human trials.

### Mucuna pruriens
- **Status**: OTC  
- **Primary outcome domains**: Physical Performance / Recovery, Stress Resilience / Mood  
- **Mechanism tags**: L-DOPA source  
- **Best evidence contexts**: Some male fertility and testosterone-adjacent signals; Parkinson’s adjunct data  
- **Evidence grade**: C  
- **Typical dose**: Standardized extracts often 300–600 mg (L-DOPA content varies).  
- **Safety**: Can interact with dopaminergic drugs. Not for long-term unsupervised use.  
- **Notes**: Natural L-DOPA. Product standardization matters.

### Boron
- **Status**: OTC  
- **Primary outcome domains**: Foundational / Nutrient Status, Physical Performance / Recovery  
- **Mechanism tags**: Trace mineral / hormone modulation (proposed)  
- **Best evidence contexts**: Small trials on free testosterone and vitamin D status  
- **Evidence grade**: C  
- **Typical dose**: 3–10 mg/day elemental boron  
- **Safety**: Generally well-tolerated at supplemental doses.  
- **Notes**: Popular in male-optimization stacks. Effects modest.

### Selenium
- **Status**: OTC  
- **Primary outcome domains**: Foundational / Nutrient Status, Longevity / Healthspan  
- **Mechanism tags**: Antioxidant enzyme cofactor (GPx)  
- **Best evidence contexts**: Deficiency correction; thyroid and immune support when low  
- **Evidence grade**: A for deficiency correction; C for supplementation above RDA in replete individuals  
- **Typical dose**: 55–200 mcg/day (avoid chronic high doses)  
- **Safety**: Narrow therapeutic window. Excess can be toxic.  
- **Notes**: Status-dependent. Soil levels vary geographically.

### L-Carnitine (plain)
- **Status**: OTC  
- **Primary outcome domains**: Physical Performance / Recovery, Foundational / Nutrient Status  
- **Mechanism tags**: Fatty acid transport / mitochondrial  
- **Best evidence contexts**: Deficiency states; some older-adult or fatigue contexts (ALCAR stronger for brain)  
- **Evidence grade**: B for certain deficiency or metabolic contexts; C for healthy young athletes  
- **Typical dose**: 1–3 g/day  
- **Safety**: Generally excellent.  
- **Notes**: Distinct from ALCAR. More peripheral than central.

### GABA
- **Status**: OTC  
- **Primary outcome domains**: Stress Resilience / Mood, Sleep Architecture  
- **Mechanism tags**: Inhibitory neurotransmitter (peripheral effects debated)  
- **Best evidence contexts**: Small trials on relaxation/sleep onset; blood-brain barrier question remains  
- **Evidence grade**: C  
- **Typical dose**: 100–500 mg/day  
- **Safety**: Generally well-tolerated.  
- **Notes**: Oral GABA may act more peripherally than centrally. Evidence mixed.

### Olive Leaf Extract (Oleuropein)
- **Status**: OTC  
- **Primary outcome domains**: Longevity / Healthspan, Foundational / Nutrient Status  
- **Mechanism tags**: Polyphenol / antioxidant / metabolic  
- **Best evidence contexts**: Blood pressure and metabolic markers in some trials  
- **Evidence grade**: C  
- **Typical dose**: 500–1000 mg extract standardized to oleuropein  
- **Safety**: Generally good.  
- **Notes**: Mediterranean diet polyphenol. Human data more modest than marketing.

---

## Interaction Matrix (Key pairs)

| Compound A | Compound B / Drug | Interaction type | Notes / Evidence | Severity |
|------------|-------------------|------------------|------------------|----------|
| Caffeine | L-Theanine | Synergistic (positive) | Multiple RCTs/meta; smoother alertness, reduced jitter [PMID: 24946991](https://pubmed.ncbi.nlm.nih.gov/24946991/) | Beneficial |
| Caffeine | Rx stimulants (methylphenidate, amphetamines) | Additive | Increased stimulation, heart rate, anxiety risk | Caution |
| L-Tyrosine | MAOIs | Potential additive monoamine | Theoretical; avoid or monitor | Caution |
| Rhodiola | MAOIs | Potential mild MAO inhibition | Preclinical; theoretical risk of elevated monoamines | Caution |
| Rhodiola | Rx stimulants | Potential additive | Mild dopaminergic/noradrenergic effects | Caution |
| Ginkgo biloba | Anticoagulants / antiplatelets (warfarin, aspirin, etc.) | Potential increased bleeding | Case reports exist; controlled trials with EGb 761 mostly negative | Caution (idiosyncratic) |
| Ashwagandha | Sedatives / CNS depressants | Potential additive sedation | Theoretical / limited | Monitor |
| Creatine | Most listed compounds | None significant | Widely studied, high safety | None |
| Citicoline / Alpha-GPC | Each other or other cholinergics | Additive cholinergic | Generally well tolerated; watch for excess cholinergic effects at high doses | Low |
| Alpha-GPC | Long-term high use | Observational stroke association | Large Korean cohort, dose-response [Lee 2021](https://jamanetwork.com/journals/jamanetworkopen/fullarticle/2786547) | Monitor / research |

**Legend**: Beneficial = supported positive interaction. Caution = monitor or avoid without clinician input. None = no meaningful interaction known.

---

## Cross-Cutting Themes

| Context / Condition | Stronger signals (Grade A/B) | Weaker signals |
|---------------------|------------------------------|----------------|
| Healthy baseline (rested) | Caffeine ± theanine, Citicoline | Most adaptogens, creatine (young), Lion’s Mane |
| Fatigue / Stress / Load | Rhodiola, Creatine, L-Tyrosine, Caffeine | — |
| Older adults / MCI | Alpha-GPC, Citicoline, Creatine, Bacopa | — |
| ADHD (core symptoms) | Methylphenidate, Amphetamines (Rx) | — |
| Sleep deprivation | Creatine, Caffeine, Modafinil (Rx) | — |

## Next Steps
- Expand compound list further (phosphatidylserine, ALCAR, huperzine A, more peptides as data matures).
- Convert interaction matrix and dual classification to structured data for app filtering.
- Add more PMIDs as literature is reviewed.
- User-specific personalization and condition response tracking live in individual logs.

**Disclaimer**: Organizational seed file based on published literature as of late 2026. Not medical advice. Personal differential responses belong in user logs.
