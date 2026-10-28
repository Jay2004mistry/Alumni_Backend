-- =============================================================================
-- Alumni Connect: Jobs & Events Seed Script
-- Purpose: Safely seed and update 20 Jobs and 15 Events assigned to valid Faculty & Alumni.
-- Non-destructive & Idempotent:
--   - Does NOT delete, truncate, or overwrite any unrelated existing records.
--   - Preserves job IDs, event IDs, descriptions, salaries, locations, and organizers.
--   - Removes unwanted " (Demo)" from job company names in-place.
--   - Updates event posters to topic-relevant image assets in-place.
--   - Uses WHERE NOT EXISTS to prevent duplicates if executed multiple times.
--   - Dynamically resolves user IDs from email addresses of existing accounts.
-- =============================================================================

BEGIN;

-- =============================================================================
-- SECTION 1: IN-PLACE CLEANUP & SEED FOR 20 JOBS
-- =============================================================================

-- Ensure existing jobs have any literal " (Demo)" removed from company_name
UPDATE jobs
SET company_name = TRIM(REPLACE(company_name, ' (Demo)', ''))
WHERE company_name LIKE '% (Demo)%';

-- Job 1: Junior Java Developer
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'Persistent Systems',
    '0-1 Years',
    '[Demo Listing] Entry-level Java Developer role for MCA graduates. Responsibilities include developing backend microservices, writing unit tests, and collaborating with cross-functional agile teams.',
    'Junior Java Developer',
    'Full Time',
    'Immediate',
    '2026-12-15',
    'Ahmedabad',
    '₹4.5 LPA',
    'Java, Spring Boot, Hibernate, MySQL, REST APIs',
    u.id,
    'careers.demo@persistent.com',
    'https://example.com/careers/demo/java-dev'
FROM users u
WHERE u.email = 'arpitjain34@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Junior Java Developer' AND company_name = 'Persistent Systems'
  );

-- Job 2: Python Developer
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'Crest Data Systems',
    '1-2 Years',
    '[Demo Listing] Seeking Python Developer to build data processing pipelines, integrate third-party APIs, and maintain scalable cloud-backed backend services.',
    'Python Developer',
    'Full Time',
    'Immediate',
    '2026-12-20',
    'Ahmedabad',
    '₹5.5 LPA',
    'Python, Django, FastAPI, PostgreSQL, Docker',
    u.id,
    'jobs.demo@crestdata.com',
    'https://example.com/careers/demo/python-dev'
FROM users u
WHERE u.email = '202500719010175@glsu.edu.in'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Python Developer' AND company_name = 'Crest Data Systems'
  );

-- Job 3: Backend Developer (Node.js & Go)
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'Simform Solutions',
    '1-3 Years',
    '[Demo Listing] Backend engineer wanted for high-throughput microservices architecture. Focus on low-latency caching, event queues, and database query optimization.',
    'Backend Developer (Node.js & Go)',
    'Full Time',
    '30 Days',
    '2026-12-30',
    'Ahmedabad',
    '₹6.5 LPA',
    'Node.js, Express, TypeScript, Go, PostgreSQL, Redis',
    u.id,
    'careers.demo@simform.com',
    'https://example.com/careers/demo/backend-dev'
FROM users u
WHERE u.email = '202500719010165@glsu.edu.in'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Backend Developer (Node.js & Go)' AND company_name = 'Simform Solutions'
  );

-- Job 4: Full Stack Developer (MERN)
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'TatvaSoft',
    '0-2 Years',
    '[Demo Listing] Looking for an energetic MERN stack developer to design responsive UI components and develop secure RESTful web APIs for enterprise web applications.',
    'Full Stack Developer (MERN)',
    'Full Time',
    'Immediate',
    '2027-01-10',
    'Ahmedabad',
    '₹5 LPA',
    'MongoDB, Express.js, React, Node.js, JavaScript ES6+, Tailwind CSS',
    u.id,
    'hr.demo@tatvasoft.com',
    'https://example.com/careers/demo/fullstack-mern'
FROM users u
WHERE u.email = '202500719010174@glsu.edu.in'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Full Stack Developer (MERN)' AND company_name = 'TatvaSoft'
  );

-- Job 5: Flutter Mobile Application Developer
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'Bacancy Technology',
    '1-2 Years',
    '[Demo Listing] Join our mobile app development team to build smooth, cross-platform Android and iOS apps with Flutter and clean state management.',
    'Flutter Mobile Application Developer',
    'Full Time',
    'Immediate',
    '2026-12-25',
    'Ahmedabad',
    '₹5.8 LPA',
    'Flutter, Dart, Provider/BLoC, Firebase, REST APIs, Git',
    u.id,
    'careers.demo@bacancy.com',
    'https://example.com/careers/demo/flutter-dev'
FROM users u
WHERE u.email = 'meetthakar34@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Flutter Mobile Application Developer' AND company_name = 'Bacancy Technology'
  );

-- Job 6: Android Developer (Kotlin & Compose)
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'Peerbits',
    '1-2 Years',
    '[Demo Listing] Native Android developer role focusing on modern declarative UI with Jetpack Compose, offline-first sync, and material design principles.',
    'Android Developer (Kotlin & Compose)',
    'Full Time',
    '15 Days',
    '2027-01-15',
    'Gandhinagar',
    '₹6 LPA',
    'Kotlin, Jetpack Compose, Coroutines, MVVM, Room DB, Retrofit',
    u.id,
    'talent.demo@peerbits.com',
    'https://example.com/careers/demo/android-dev'
FROM users u
WHERE u.email = 'harshposhiya@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Android Developer (Kotlin & Compose)' AND company_name = 'Peerbits'
  );

-- Job 7: Frontend Developer (React & TypeScript)
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'Radixweb',
    '0-2 Years',
    '[Demo Listing] Design and build clean, accessible frontend dashboards. Must have solid fundamentals in modern JavaScript/TypeScript and component lifecycle.',
    'Frontend Developer (React & TypeScript)',
    'Full Time',
    'Immediate',
    '2026-12-18',
    'Ahmedabad',
    '₹4.8 LPA',
    'React 18, TypeScript, Redux Toolkit, HTML5, CSS3, Webpack',
    u.id,
    'careers.demo@radixweb.com',
    'https://example.com/careers/demo/frontend-dev'
FROM users u
WHERE u.email = 'sujalchovatiya@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Frontend Developer (React & TypeScript)' AND company_name = 'Radixweb'
  );

-- Job 8: Data Analyst Trainee
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'eInfochips',
    'Fresher',
    '[Demo Listing] Great opportunity for fresh MCA graduates with an analytical mindset to clean, transform, and analyze datasets to drive business insights.',
    'Data Analyst Trainee',
    'Full Time',
    'Immediate',
    '2027-01-05',
    'Ahmedabad',
    '₹4.2 LPA',
    'SQL, Python, Pandas, Power BI, Excel, Data Visualization',
    u.id,
    'jobs.demo@einfochips.com',
    'https://example.com/careers/demo/data-analyst'
FROM users u
WHERE u.email = 'devratvyas@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Data Analyst Trainee' AND company_name = 'eInfochips'
  );

-- Job 9: Machine Learning Intern
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'Argusoft India',
    'Internship',
    '[Demo Listing] 6-month paid internship working alongside research scientists on natural language processing, model evaluation, and feature engineering.',
    'Machine Learning Intern',
    'Internship',
    'Immediate',
    '2026-11-30',
    'Gandhinagar',
    '₹2.5 LPA',
    'Python, Scikit-Learn, PyTorch, NumPy, Data Preprocessing, NLP',
    u.id,
    'internships.demo@argusoft.com',
    'https://example.com/careers/demo/ml-intern'
FROM users u
WHERE u.email = 'vivekposhiya82@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Machine Learning Intern' AND company_name = 'Argusoft India'
  );

-- Job 10: Software Engineer - Core Services
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'Cygnet Digital',
    '1-3 Years',
    '[Demo Listing] Build and maintain enterprise service layers and integration bus solutions. Responsible for high code quality, automated tests, and deployment.',
    'Software Engineer - Core Services',
    'Full Time',
    '30 Days',
    '2027-01-20',
    'Ahmedabad',
    '₹7.2 LPA',
    'Core Java, C#, SQL Server, Microservices, CI/CD, Git',
    u.id,
    'careers.demo@cygnetdigital.com',
    'https://example.com/careers/demo/software-engineer'
FROM users u
WHERE u.email = 'vrajpopat@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Software Engineer - Core Services' AND company_name = 'Cygnet Digital'
  );

-- Job 11: Spring Boot Microservices Developer
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'Infosys Technologies',
    '1-3 Years',
    '[Demo Listing] Join our cloud services team designing distributed cloud-native microservices with Spring Cloud, asynchronous messaging, and container deployment.',
    'Spring Boot Microservices Developer',
    'Full Time',
    '30 Days',
    '2027-01-25',
    'Pune',
    '₹8 LPA',
    'Java 17, Spring Boot, Spring Cloud, Kafka, Docker, Kubernetes',
    u.id,
    'careers.demo@infosys.com',
    'https://example.com/careers/demo/springboot-dev'
FROM users u
WHERE u.email = 'parthsoni@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Spring Boot Microservices Developer' AND company_name = 'Infosys Technologies'
  );

-- Job 12: React Developer (Next.js & Web)
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'Mindinventory',
    '1-2 Years',
    '[Demo Listing] Seeking a frontend developer skilled in React and Next.js to craft high-performance, SEO-friendly web apps with modern web vitals.',
    'React Developer (Next.js & Web)',
    'Full Time',
    'Immediate',
    '2026-12-28',
    'Ahmedabad',
    '₹5.2 LPA',
    'React.js, Next.js, JavaScript, Tailwind CSS, SSR, REST APIs',
    u.id,
    'jobs.demo@mindinventory.com',
    'https://example.com/careers/demo/react-dev'
FROM users u
WHERE u.email = 'aanshishah@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'React Developer (Next.js & Web)' AND company_name = 'Mindinventory'
  );

-- Job 13: Junior DevOps Engineer
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'Gateway Group',
    '0-2 Years',
    '[Demo Listing] Junior DevOps engineer to maintain continuous delivery pipelines, monitor production cloud environments, and script infrastructure tasks.',
    'Junior DevOps Engineer',
    'Full Time',
    'Immediate',
    '2027-01-30',
    'Ahmedabad',
    '₹6 LPA',
    'Linux, Docker, Jenkins, GitHub Actions, AWS Basics, Bash Scripting',
    u.id,
    'careers.demo@gatewaygroup.com',
    'https://example.com/careers/demo/devops-engineer'
FROM users u
WHERE u.email = 'vpposhiya@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Junior DevOps Engineer' AND company_name = 'Gateway Group'
  );

-- Job 14: QA / Automation Test Engineer
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'Zeus Learning',
    '0-2 Years',
    '[Demo Listing] Create automated test scripts for web applications, execute regression test suites, and report defects with thorough reproduction steps.',
    'QA / Automation Test Engineer',
    'Full Time',
    'Immediate',
    '2026-12-31',
    'Remote',
    '₹4.5 LPA',
    'Selenium WebDriver, Java/Python, TestNG, Postman, JIRA, SQL',
    u.id,
    'careers.demo@zeuslearning.com',
    'https://example.com/careers/demo/qa-engineer'
FROM users u
WHERE u.email = 'karanvisani@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'QA / Automation Test Engineer' AND company_name = 'Zeus Learning'
  );

-- Job 15: Database Developer & SQL Specialist
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'Prompt Softech',
    '1-2 Years',
    '[Demo Listing] Responsible for database schema design, indexing strategies, complex query tuning, and ensuring high database availability and data integrity.',
    'Database Developer & SQL Specialist',
    'Full Time',
    '15 Days',
    '2027-02-10',
    'Ahmedabad',
    '₹5 LPA',
    'PostgreSQL, MySQL, PL/SQL, Query Optimization, Stored Procedures, Indexing',
    u.id,
    'hr.demo@promptsoftech.com',
    'https://example.com/careers/demo/db-developer'
FROM users u
WHERE u.email = 'dharmikbadheka@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Database Developer & SQL Specialist' AND company_name = 'Prompt Softech'
  );

-- Job 16: Associate Cloud Engineer
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'Volansys Technologies',
    '1-2 Years',
    '[Demo Listing] Configure and monitor cloud infrastructure on AWS. Support system administrators with infrastructure-as-code and backup routines.',
    'Associate Cloud Engineer',
    'Full Time',
    'Immediate',
    '2027-02-15',
    'Bengaluru (Bangalore)',
    '₹7 LPA',
    'AWS, EC2, S3, IAM, CloudWatch, Terraform, Python Scripting',
    u.id,
    'careers.demo@volansys.com',
    'https://example.com/careers/demo/cloud-engineer'
FROM users u
WHERE u.email = 'devarshimehta34@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Associate Cloud Engineer' AND company_name = 'Volansys Technologies'
  );

-- Job 17: Web Developer (PHP & Laravel)
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'SPEC INDIA',
    '0-2 Years',
    '[Demo Listing] Develop and maintain web portals using Laravel framework. Build modular MVC controllers, database migrations, and clean blade templates.',
    'Web Developer (PHP & Laravel)',
    'Full Time',
    'Immediate',
    '2026-12-22',
    'Ahmedabad',
    '₹4 LPA',
    'PHP 8, Laravel, MySQL, JavaScript, HTML5, Bootstrap, Git',
    u.id,
    'jobs.demo@spec-india.com',
    'https://example.com/careers/demo/web-dev-laravel'
FROM users u
WHERE u.email = 'dhruvprajapati@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Web Developer (PHP & Laravel)' AND company_name = 'SPEC INDIA'
  );

-- Job 18: Technical Business Analyst
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'OpenXcell',
    '1-3 Years',
    '[Demo Listing] Bridge the gap between business clients and software development teams by translating functional requirements into clear technical specifications.',
    'Technical Business Analyst',
    'Full Time',
    '30 Days',
    '2027-02-20',
    'Ahmedabad',
    '₹6 LPA',
    'Requirement Gathering, User Stories, UML Diagrams, Agile/Scrum, Wireframing',
    u.id,
    'careers.demo@openxcell.com',
    'https://example.com/careers/demo/business-analyst'
FROM users u
WHERE u.email = 'krupamehta34@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Technical Business Analyst' AND company_name = 'OpenXcell'
  );

-- Job 19: Software Development Intern
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'Hidden Brains',
    'Internship',
    '[Demo Listing] Fast-paced 6-month internship for MCA students. Hands-on coding on production-grade features under the guidance of senior mentors.',
    'Software Development Intern',
    'Internship',
    'Immediate',
    '2026-11-28',
    'Ahmedabad',
    '₹2.4 LPA',
    'Data Structures, Algorithms, Java/Python, OOP Concepts, Web Basics',
    u.id,
    'interns.demo@hiddenbrains.com',
    'https://example.com/careers/demo/dev-intern'
FROM users u
WHERE u.email = 'devarshprajapati@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Software Development Intern' AND company_name = 'Hidden Brains'
  );

-- Job 20: Graduate Trainee Engineer
INSERT INTO jobs (
    company_name, experience_required, job_description, job_title, job_type,
    joining_type, last_date_to_apply, location, salary, skills_required,
    user_id, company_email, company_link
)
SELECT
    'Tata Consultancy Services',
    'Fresher',
    '[Demo Listing] Structured graduate entry program offering training across emerging technologies, enterprise architecture, and client software solutions.',
    'Graduate Trainee Engineer',
    'Full Time',
    'Immediate',
    '2027-01-31',
    'Gandhinagar',
    '₹3.8 LPA',
    'C, C++, Java, RDBMS, Problem Solving, Software Engineering Concepts',
    u.id,
    'campus.demo@tcs.com',
    'https://example.com/careers/demo/graduate-trainee'
FROM users u
WHERE u.email = 'harshmakvana@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM jobs WHERE job_title = 'Graduate Trainee Engineer' AND company_name = 'Tata Consultancy Services'
  );


-- =============================================================================
-- SECTION 2: IN-PLACE IMAGE UPDATES & SEED FOR 15 EVENTS
-- =============================================================================

-- In-place update of topic-relevant images for all 15 events
UPDATE event SET image_url = '/uploads/events/event_alumni_networking.jpg' WHERE title = 'Alumni Networking & Mentorship Meet 2026';
UPDATE event SET image_url = '/uploads/events/event_java_springboot.jpg' WHERE title = 'Java & Spring Boot Enterprise Masterclass';
UPDATE event SET image_url = '/uploads/events/event_python_workshop.jpg' WHERE title = 'Python for Modern Software Engineering Workshop';
UPDATE event SET image_url = '/uploads/events/event_machine_learning.jpg' WHERE title = 'Machine Learning & AI in Production Seminar';
UPDATE event SET image_url = '/uploads/events/event_blockchain.jpg' WHERE title = 'Blockchain Technology & Smart Contracts Session';
UPDATE event SET image_url = '/uploads/events/event_flutter_workshop.jpg' WHERE title = 'Flutter Cross-Platform App Development Workshop';
UPDATE event SET image_url = '/uploads/events/event_angular_web.jpg' WHERE title = 'Modern Angular & Web Development Workshop';
UPDATE event SET image_url = '/uploads/events/event_resume_interview.jpg' WHERE title = 'Resume Building & Tech Interview Preparation';
UPDATE event SET image_url = '/uploads/events/event_mock_interview.jpg' WHERE title = 'Technical Mock Interview Day for MCA Finalists';
UPDATE event SET image_url = '/uploads/events/event_hackathon.jpg' WHERE title = 'GLS Hackathon 2027: Code for Innovation';
UPDATE event SET image_url = '/uploads/events/event_microservices.jpg' WHERE title = 'Industry Expert Guest Lecture: Microservices at Scale';
UPDATE event SET image_url = '/uploads/events/event_career_guidance.jpg' WHERE title = 'Career Guidance & Higher Studies Interactive Session';
UPDATE event SET image_url = '/uploads/events/event_data_analytics.jpg' WHERE title = 'Hands-on Data Analytics & Power BI Workshop';
UPDATE event SET image_url = '/uploads/events/event_cloud_aws.jpg' WHERE title = 'Cloud Computing & AWS Architecture Seminar';
UPDATE event SET image_url = '/uploads/events/event_mentorship_roundtable.jpg' WHERE title = 'Alumni Career & Mentorship Round Table';

-- Event 1: Alumni Networking & Mentorship Meet 2026
INSERT INTO event (
    title, description, location, event_date, status,
    created_at, created_by, image_url, note, target_department
)
SELECT
    'Alumni Networking & Mentorship Meet 2026',
    'Connect with GLS MCA alumni working across leading tech companies. Gain career insights, discuss industry hiring trends, and explore one-on-one mentorship opportunities.',
    'GLS Auditorium, GLS Campus, Ahmedabad',
    '2026-11-21',
    'APPROVED',
    '2026-10-28 10:00:00',
    u.id,
    '/uploads/events/event_alumni_networking.jpg',
    'Annual networking session connecting recent MCA graduates with alumni mentors across IT industries.',
    'MCA'
FROM users u
WHERE u.email = '202500719010175@glsu.edu.in'
  AND NOT EXISTS (
      SELECT 1 FROM event WHERE title = 'Alumni Networking & Mentorship Meet 2026'
  );

-- Event 2: Java & Spring Boot Enterprise Masterclass
INSERT INTO event (
    title, description, location, event_date, status,
    created_at, created_by, image_url, note, target_department
)
SELECT
    'Java & Spring Boot Enterprise Masterclass',
    'Intensive technical session covering Spring Boot 3 architecture, dependency injection, JPA/Hibernate performance tips, and securing REST APIs with JWT tokens.',
    'MCA Computer Lab 1, GLS University',
    '2026-11-28',
    'APPROVED',
    '2026-10-28 10:00:00',
    u.id,
    '/uploads/events/event_java_springboot.jpg',
    'Hands-on coding workshop. Please bring laptops with JDK 17 installed.',
    'MCA'
FROM users u
WHERE u.email = 'arpitjain34@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM event WHERE title = 'Java & Spring Boot Enterprise Masterclass'
  );

-- Event 3: Python for Modern Software Engineering Workshop
INSERT INTO event (
    title, description, location, event_date, status,
    created_at, created_by, image_url, note, target_department
)
SELECT
    'Python for Modern Software Engineering Workshop',
    'Master clean Python programming, type annotations, asynchronous programming with asyncio, and building lightweight microservices using FastAPI.',
    'MCA Seminar Hall A, Ahmedabad',
    '2026-12-05',
    'APPROVED',
    '2026-10-28 10:00:00',
    u.id,
    '/uploads/events/event_python_workshop.jpg',
    'Open to all MCA batches. Includes practical code walkthroughs.',
    'MCA'
FROM users u
WHERE u.email = '202500719010165@glsu.edu.in'
  AND NOT EXISTS (
      SELECT 1 FROM event WHERE title = 'Python for Modern Software Engineering Workshop'
  );

-- Event 4: Machine Learning & AI in Production Seminar
INSERT INTO event (
    title, description, location, event_date, status,
    created_at, created_by, image_url, note, target_department
)
SELECT
    'Machine Learning & AI in Production Seminar',
    'Exploring end-to-end Machine Learning pipelines from data cleaning and model training with PyTorch to deploying ML models using Docker and FastAPI endpoints.',
    'Central Conference Hall, GLS University',
    '2026-12-12',
    'APPROVED',
    '2026-10-28 10:00:00',
    u.id,
    '/uploads/events/event_machine_learning.jpg',
    'Prerequisites: Basic knowledge of Python and mathematics.',
    'MCA'
FROM users u
WHERE u.email = 'vivekposhiya82@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM event WHERE title = 'Machine Learning & AI in Production Seminar'
  );

-- Event 5: Blockchain Technology & Smart Contracts Session
INSERT INTO event (
    title, description, location, event_date, status,
    created_at, created_by, image_url, note, target_department
)
SELECT
    'Blockchain Technology & Smart Contracts Session',
    'Understand decentralized architecture, consensus algorithms, Ethereum basics, and writing basic Solidity smart contracts with web3 integration.',
    'Online via Google Meet',
    '2026-12-19',
    'APPROVED',
    '2026-10-28 10:00:00',
    u.id,
    '/uploads/events/event_blockchain.jpg',
    'Link will be shared with registered participants.',
    'MCA'
FROM users u
WHERE u.email = 'arpitjain34@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM event WHERE title = 'Blockchain Technology & Smart Contracts Session'
  );

-- Event 6: Flutter Cross-Platform App Development Workshop
INSERT INTO event (
    title, description, location, event_date, status,
    created_at, created_by, image_url, note, target_department
)
SELECT
    'Flutter Cross-Platform App Development Workshop',
    'Practical workshop on building expressive, native-compiled mobile apps for iOS and Android using Flutter 3, BLoC pattern, and REST API integration.',
    'MCA Mobile Lab, GLS Campus',
    '2027-01-09',
    'APPROVED',
    '2026-10-28 10:00:00',
    u.id,
    '/uploads/events/event_flutter_workshop.jpg',
    'Install Flutter SDK and Android Studio prior to the workshop.',
    'MCA'
FROM users u
WHERE u.email = 'meetthakar34@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM event WHERE title = 'Flutter Cross-Platform App Development Workshop'
  );

-- Event 7: Modern Angular & Web Development Workshop
INSERT INTO event (
    title, description, location, event_date, status,
    created_at, created_by, image_url, note, target_department
)
SELECT
    'Modern Angular & Web Development Workshop',
    'Deep dive into Angular signals, standalone components, reactive forms, routing strategies, and consuming secure backend microservices.',
    'MCA Lab 3, GLS University',
    '2027-01-16',
    'APPROVED',
    '2026-10-28 10:00:00',
    u.id,
    '/uploads/events/event_angular_web.jpg',
    'Intermediate web developers. Covers standalone components.',
    'MCA'
FROM users u
WHERE u.email = 'meetthakar34@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM event WHERE title = 'Modern Angular & Web Development Workshop'
  );

-- Event 8: Resume Building & Tech Interview Preparation
INSERT INTO event (
    title, description, location, event_date, status,
    created_at, created_by, image_url, note, target_department
)
SELECT
    'Resume Building & Tech Interview Preparation',
    'Faculty and alumni career counselors provide actionable feedback on crafting ATS-compliant technical resumes, formatting project highlights, and behavioral interview techniques.',
    'GLS Auditorium, Ahmedabad',
    '2027-01-23',
    'APPROVED',
    '2026-10-28 10:00:00',
    u.id,
    '/uploads/events/event_resume_interview.jpg',
    'Bring printed copies of your draft resume for live review.',
    'MCA'
FROM users u
WHERE u.email = 'vpposhiya@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM event WHERE title = 'Resume Building & Tech Interview Preparation'
  );

-- Event 9: Technical Mock Interview Day for MCA Finalists
INSERT INTO event (
    title, description, location, event_date, status,
    created_at, created_by, image_url, note, target_department
)
SELECT
    'Technical Mock Interview Day for MCA Finalists',
    'One-on-one technical mock interviews simulating real corporate campus placements in Data Structures, Java, Web technologies, and System Design.',
    'Department Interview Rooms, GLS Campus',
    '2027-01-30',
    'APPROVED',
    '2026-10-28 10:00:00',
    u.id,
    '/uploads/events/event_mock_interview.jpg',
    'One-on-one 45-minute simulation slots with industry alumni.',
    'MCA'
FROM users u
WHERE u.email = '202500719010174@glsu.edu.in'
  AND NOT EXISTS (
      SELECT 1 FROM event WHERE title = 'Technical Mock Interview Day for MCA Finalists'
  );

-- Event 10: GLS Hackathon 2027: Code for Innovation
INSERT INTO event (
    title, description, location, event_date, status,
    created_at, created_by, image_url, note, target_department
)
SELECT
    'GLS Hackathon 2027: Code for Innovation',
    '24-hour hackathon challenging students to build innovative software solutions for smart campuses, healthcare, and education using AI, Web, and Mobile tech.',
    'Main IT Building, GLS University',
    '2027-02-06',
    'APPROVED',
    '2026-10-28 10:00:00',
    u.id,
    '/uploads/events/event_hackathon.jpg',
    'Team size: 2 to 4 students. 24-hour hackathon with cash prizes.',
    'MCA'
FROM users u
WHERE u.email = 'harshposhiya@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM event WHERE title = 'GLS Hackathon 2027: Code for Innovation'
  );

-- Event 11: Industry Expert Guest Lecture: Microservices at Scale
INSERT INTO event (
    title, description, location, event_date, status,
    created_at, created_by, image_url, note, target_department
)
SELECT
    'Industry Expert Guest Lecture: Microservices at Scale',
    'Guest tech leads share case studies on managing distributed transaction patterns, Kafka event streams, and fault-tolerant cloud architecture in high-traffic platforms.',
    'Seminar Hall 2, GLS University',
    '2027-02-13',
    'APPROVED',
    '2026-10-28 10:00:00',
    u.id,
    '/uploads/events/event_microservices.jpg',
    'Includes Q&A with Senior Architects.',
    'MCA'
FROM users u
WHERE u.email = 'devarshimehta34@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM event WHERE title = 'Industry Expert Guest Lecture: Microservices at Scale'
  );

-- Event 12: Career Guidance & Higher Studies Interactive Session
INSERT INTO event (
    title, description, location, event_date, status,
    created_at, created_by, image_url, note, target_department
)
SELECT
    'Career Guidance & Higher Studies Interactive Session',
    'Guidance session for MCA students exploring industry certifications (AWS, Azure, Java), post-graduation research, and domestic vs. global tech career tracks.',
    'Auditorium Hall B, GLS Campus',
    '2027-02-20',
    'APPROVED',
    '2026-10-28 10:00:00',
    u.id,
    '/uploads/events/event_career_guidance.jpg',
    'Covers certifications, research opportunities, and placements.',
    'MCA'
FROM users u
WHERE u.email = 'krupamehta34@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM event WHERE title = 'Career Guidance & Higher Studies Interactive Session'
  );

-- Event 13: Hands-on Data Analytics & Power BI Workshop
INSERT INTO event (
    title, description, location, event_date, status,
    created_at, created_by, image_url, note, target_department
)
SELECT
    'Hands-on Data Analytics & Power BI Workshop',
    'Hands-on session on modeling business datasets, building interactive Power BI dashboards, and extracting actionable trends with DAX and SQL queries.',
    'MCA Lab 2, GLS University',
    '2027-02-27',
    'APPROVED',
    '2026-10-28 10:00:00',
    u.id,
    '/uploads/events/event_data_analytics.jpg',
    'Power BI Desktop free version will be used.',
    'MCA'
FROM users u
WHERE u.email = 'girishdave34@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM event WHERE title = 'Hands-on Data Analytics & Power BI Workshop'
  );

-- Event 14: Cloud Computing & AWS Architecture Seminar
INSERT INTO event (
    title, description, location, event_date, status,
    created_at, created_by, image_url, note, target_department
)
SELECT
    'Cloud Computing & AWS Architecture Seminar',
    'Overview of cloud computing paradigms, comparing IaaS, PaaS, and SaaS, and setting up scalable compute (EC2), storage (S3), and VPC networks on AWS.',
    'Seminar Hall 1, GLS University',
    '2027-03-06',
    'APPROVED',
    '2026-10-28 10:00:00',
    u.id,
    '/uploads/events/event_cloud_aws.jpg',
    'Covers AWS Free Tier hands-on demonstrations.',
    'MCA'
FROM users u
WHERE u.email = 'darshitakalyani34@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM event WHERE title = 'Cloud Computing & AWS Architecture Seminar'
  );

-- Event 15: Alumni Career & Mentorship Round Table
INSERT INTO event (
    title, description, location, event_date, status,
    created_at, created_by, image_url, note, target_department
)
SELECT
    'Alumni Career & Mentorship Round Table',
    'Round-table networking event connecting MCA final year students with senior alumni working as Tech Leads, Product Managers, and Entrepreneurs.',
    'Alumni Lounge, GLS Campus, Ahmedabad',
    '2027-03-13',
    'APPROVED',
    '2026-10-28 10:00:00',
    u.id,
    '/uploads/events/event_mentorship_roundtable.jpg',
    'Informal group discussions and networking tea.',
    'MCA'
FROM users u
WHERE u.email = 'jyotsnakhatri34@gmail.com'
  AND NOT EXISTS (
      SELECT 1 FROM event WHERE title = 'Alumni Career & Mentorship Round Table'
  );

-- =============================================================================
-- SECTION 3: VERIFICATION QUERIES
-- =============================================================================

SELECT 'TOTAL JOBS' AS metric, COUNT(*) AS count FROM jobs;
SELECT 'JOBS WITH (Demo)' AS metric, COUNT(*) AS count FROM jobs WHERE company_name LIKE '%(Demo)%';
SELECT 'TOTAL EVENTS' AS metric, COUNT(*) AS count FROM event;
SELECT 'EVENTS WITH UPDATED TOPIC IMAGES' AS metric, COUNT(*) AS count FROM event WHERE image_url LIKE '/uploads/events/event_%';

COMMIT;
