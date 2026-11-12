# Getting Started

**Before the workshop** you completed **M0** in your *pre-flight project* (`substream-preflight`, in the GitLab group with your name): the tools installed, the libraries downloaded, your access token created, Git told who you are, and your first Merge Request merged with the NIX bot's comment. Not done yet? Do it now, with a mentor: its `README.md` is the guide. It no longer earns points, but you need it to work.

**At the workshop** this guide starts: you clone this project, the challenge, and open it.

---

## 1. What you prepared in the pre-flight project

| Tool | Version | Check with |
| :--- | :--- | :--- |
| JDK | 17 or newer | `java -version` |
| Maven | 3.9 or newer | `mvn -version` |
| IntelliJ IDEA | Community edition is enough | — |
| Git | any recent version | `git --version` |

Your **access token** is the password Git asks for. Git knows your name and email (`git config --global user.name` and `user.email`).

---

## 2. Clone and Open

```bash
git clone <your repository URL from GitLab>
cd substream-campus
```

When Git asks for a password, paste your **access token**.

In IntelliJ: **File → Open**, select the folder that **directly contains `pom.xml`**, and choose **Trust project**. The libraries are already on your laptop from the pre-flight project, so this takes seconds.

It worked if `src/main/java` is blue, `src/test/java` is green, and classes have a round **C** icon. If not: right-click `pom.xml` → **Maven → Reload project**. Also check **File → Project Structure → SDK** is 17 or newer.

Every milestone works as M0 did: a branch with the milestone's name (`docs/SUBMISSION_RULES.md`, section 2), commits, a Merge Request into `main`, the pipeline, the NIX bot's comment, the merge.

---

## 3. Build and Run the Tests

In IntelliJ: right-click `src/test/java` → **Run 'All Tests'**.
In a terminal (optional): `mvn clean verify`.

On a fresh project **the structure tests and `SubscriptionPlanTest` fail on purpose.** The structure tests are in `src/test/java/com/substream/structure/`, one per milestone. `M1StructureTest` lists everything M1 still needs, for example:

```
Customer does not match README section 4:
  - Missing public method Customer.getName() : String
  - Missing field Customer.name
```

That list is your M1 checklist. `SubscriptionPlanTest` is a provided example test: when it passes, your `SubscriptionPlan` is right. `M2StructureTest` and `M3StructureTest` stay red until you reach those milestones: ignore them until then. `ServicesStructureTest` (Developer track) is green from the start.

A structure test checks names, parameters, return types, private fields and that there are no setters. It does not check the logic: the NIX bot checks the rules on every Merge Request.

Every unfinished method throws `UnsupportedOperationException("TODO: …")` naming the rule to implement. **View → Tool Windows → TODO** shows all of them.

---

## 4. Troubleshooting

| Problem | Fix |
| :--- | :--- |
| `Authentication failed` on push | Use the access token as the password. On Windows, update it in **Credential Manager**. |
| IntelliJ shows plain coffee-cup icons | Right-click `pom.xml` → **Maven → Reload project**. |
| `release version 17 not supported` | IntelliJ uses an older JDK: **File → Project Structure → SDK**. |
| Dependencies never finish downloading | Check your internet connection or proxy, then **Maven → Reload project**. |
| Pipeline says *REJECTED: protected file* | You changed a provided file. Restore it: `git checkout origin/main -- <file>`, commit, push. |
