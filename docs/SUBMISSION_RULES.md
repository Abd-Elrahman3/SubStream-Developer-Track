# Submission Rules

These rules are strict because an automated pipeline checks them, exactly as in a professional team. A rule broken by accident still counts as broken, so read this page once, carefully.

---

## 1. The Workflow, for Every Milestone

```bash
git checkout main
git pull                                    # always start from the latest main
git checkout -b feature/<milestone-branch>  # exact name from section 2
# ... work, and commit in small steps ...
git push -u origin feature/<milestone-branch>
```

Then on GitLab:

1. Open a **Merge Request** from your branch into `main`. Title: `<Milestone>: <short summary>`, for example `M3: plan changes and lifecycle`.
2. Wait for the pipeline. A few minutes later the NIX bot comments on the Merge Request: a **Structure** line (your milestone's structure test), then one line per rule of the milestone, marked pass or fail. Fix and push again; the bot checks each new commit from scratch: every rule of the milestone again, also the ones that passed before, because a change can break them.
3. **Merge** it. Keep the branch (do not tick *Delete source branch*).
4. Start the next milestone from the updated `main`.

| Pipeline result | Meaning | Can you merge? |
| :--- | :--- | :--- |
| `rules-check` red | Wrong branch name, wrong target, or a protected file changed | No |
| `compile` red | The code in `src/main/java` does not compile | No |
| Both green, bot comment shows ❌ | Some rules of the milestone fail | **Yes**, but it will most likely cost points. Fix them, or merge and move on |
| Both green, bot comment all ✅ | Every rule checked by the bot passes | Yes |

### Before you merge

1. Run the structure test of your milestone on your machine, for example `M1StructureTest` (in `src/test/java/com/substream/structure/`). It must be green: if a name or a signature differs from README section 4, the rule checks cannot use your classes at all.
2. Wait for the bot's comment and fix what you can.
3. Merge. A rule marked ❌ never blocks the merge, but it will most likely cost points: the final evaluation checks the same rules with more cases than the README shows. Each milestone is scored on its own, so a mistake costs points only in the milestone where it happens.

The bot uses the examples of the README only. Think about the limits yourself; you may write your own tests for them (optional, not graded).

You cannot push to `main` directly. Stuck on one rule for more than an hour? Merge what you have and move on; you can come back later with a new Merge Request from the same branch.

### What you see after each push

| When | Where on the Merge Request | What you see |
| :--- | :--- | :--- |
| At once | *Pipelines* tab, and the status at the top | The pipeline starts: `rules-check`, then `compile`. It takes a few minutes at most |
| Within a few minutes | *Overview* tab, in the comments | ⏳ **milestone-check** is checking commit `1a2b3c4d`. |
| A few minutes later | The same comment | Replaced by the result of that commit |

Every new commit you push gets its own comment; the comments of earlier commits stay. The bot comment can arrive before or after the pipeline finishes. If neither has changed after 20 minutes, tell the NIX team.

**If the pipeline is red**, the Merge Request says *Merge blocked: Pipeline must succeed*. Open the red job; its last lines say why:

| Job and message | What to do |
| :--- | :--- |
| `rules-check`: `REJECTED: '...' is not an allowed branch name` | Push your work to a branch with the exact name of section 2 and open a new Merge Request |
| `rules-check`: `REJECTED: protected file(s) modified` | A file you changed: restore it with `git checkout origin/main -- <file>`. A file you added in a protected folder: move it (tests belong in `src/test/java`) |
| `compile`: `...java:12: error: ...` | Fix the file and line it names |

The pipeline only compiles your code: no test runs in it. Your tests and the structure test of your milestone run on your machine (`docs/GETTING_STARTED.md`, section 3) and in the NIX bot, which reports them in its comment.

**What the bot comment says**

| You see | Meaning |
| :--- | :--- |
| **Structure** (for example `M1StructureTest`) | ✅ Your classes match README section 4. ❌ Names the classes that differ: run that test on your machine to see the list |
| A table *Rule · What is checked · Result* | One row per rule of the milestone: ✅ Pass, ❌ Fail, — Not checked, ⏱️ Did not finish (look for a loop that never ends), ⏸️ Needs another rule of this milestone first (its check uses a method of another rule that does not work yet, for example `cancel()` to make a cancelled subscription) |
| *Your tests on a correct build of ...* | All your own tests in `src/test/java` (from every milestone so far), run against correct code. The number is how many ran; ✅ means none is a **false alarm**. ❌ means a false alarm: the test is wrong, not the code. Re-read the rule and fix the test |
| *Required test class* (Developer track) | Whether the test class your milestone asks for is there under its exact name (`TRACK_DEVELOPER.md`, *Your tests*). ❌ *not found*: check the name and the folder, or those tests score 0 |
| ❌ *The checks did not run* | A file of the milestone is missing from your branch, or your code uses something SubStream never needs (`System.exit`, reflection, file access, ...). The comment names the file and line |
| ❌ *Your code does not compile together with the rest of SubStream* | A public name or signature differs from README section 4. Run the structure test of your milestone on your machine: it lists the differences |
| ⚠️ *a problem on the NIX side* | Nothing is wrong with your Merge Request. NIX fixes it; the next commit is checked as usual |
| *... is not a milestone branch* | The branch is not one of section 2, so there is nothing to check |

A ❌ in the bot comment never blocks the merge. Only a red pipeline does.

---

## 2. Branch Names

Use exactly these names. Any other name is rejected.

| Milestone | Branch |
| :--- | :--- |
| M0 | `feature/m0-preflight`, in your pre-flight project (before the workshop) |
| M1 – M3 (everyone) | `feature/m1-domain`, `feature/m2-invoices`, `feature/m3-subscription` |
| D4 – D7 (Developer) | `feature/d4-service`, `feature/d5-billing`, `feature/d6-failures`, `feature/d7-reporting` |
| A4 – A7 (AQA Engineer) | `feature/a4-unit-tests`, `feature/a5-service-tests`, `feature/a6-api-tests`, `feature/a7-billing-api-tests` |
| Reflection | `feature/reflection` (any time before the deadline) |

The first branch you open after M3 (`d4` or `a4`) is your track choice. It is final: branches of the other track are not evaluated.

---

## 3. Commits

- Commit in **small steps** with messages that say what changed: `M3: reject downgrade when seats exceed limit`.
- Start every message with the milestone code (`M1:`, `M3:`, `D5:`, `A6:` …).
- One large commit with the whole milestone is allowed, but it is a weaker signal of how you work.
- Commit with your own name and registered email (you set them in the pre-flight project, M0).

---

## 4. What You Must Not Change

The pipeline rejects any Merge Request that changes these files:

- `pom.xml`, `.gitlab-ci.yml`, `SubscriptionPlanTest`, everything in `src/test/java/com/substream/structure/`
- every file marked **PROVIDED — DO NOT MODIFY**
- everything in `src/main/java/com/substream/aqa/` (SubStream Pricing) and its two example tests

Also keep every public class, constructor, method and field name exactly as specified. You may add private fields, private methods and new classes.

---

## 5. In the Workshop: the M1 Merge Request

Before the end of the workshop you will push `feature/m1-domain` and open its Merge Request, **finished or not**. It may be red at that moment; you continue on the same branch at home and merge it once it compiles and you are happy with it.

---

## 6. Deadline

- The deadline is in your invitation email and is checked against the **server's clock**.
- Only work **merged into `main`** before the deadline counts. Open Merge Requests are not evaluated.
- Later pushes are ignored. Plan to merge at least an hour early.

---

## 7. Honesty

- Your work must be your own. You must be able to explain, and change, every line you submit.
- Copying another participant's code is not allowed. Near-identical solutions are disqualified, all of them.
- Fill in `REFLECTION.md` honestly.
