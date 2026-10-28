-- =============================================================================
-- Alumni Connect: Alumni & Faculty Dataset Replacement Seed Script
-- Generated: 2026-10-28
-- Purpose:  Remove old demo alumni/faculty records and seed 15 intended alumni
--           and 9 intended faculty members.
-- Idempotent: safe to run multiple times; uses INSERT … ON CONFLICT DO UPDATE
-- =============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- SECTION 0: Identify roles
-- ---------------------------------------------------------------------------
-- role_id 1 = ALUMNI, 2 = ADMIN, 3 = FACULTY, 4 = STUDENT  (from DB inspection)

-- ---------------------------------------------------------------------------
-- SECTION 1: Collect IDs of OLD demo alumni to remove
--   Old alumni = all ALUMNI users EXCEPT:
--     id 120 (Vivek Poshiya, 202500719010175@glsu.edu.in) — KEEP as alumni
--     id 112 (vivekposhiya82@gmail.com)  — will be re-used as Prerna Agarwal FACULTY
--     id 114 (vpposhiya@gmail.com)       — will be re-used as Tushar Patel FACULTY
--   (ids 46, 49-79, 115, 117, 119 are old demo records to remove)
-- ---------------------------------------------------------------------------

-- 1a. Delete jobs created by old demo ALUMNI users (before deleting those users)
DELETE FROM jobs
WHERE user_id IN (
    SELECT id FROM users
    WHERE role_id = 1
      AND id NOT IN (112, 114, 120)
);

-- 1b. Delete posts created by old demo ALUMNI users
DELETE FROM posts
WHERE user_id IN (
    SELECT id FROM users
    WHERE role_id = 1
      AND id NOT IN (112, 114, 120)
);

-- 1c. Delete events created by old demo ALUMNI users
DELETE FROM event
WHERE created_by IN (
    SELECT id FROM users
    WHERE role_id = 1
      AND id NOT IN (112, 114, 120)
);

-- 1d. Delete alumni_profile rows for old demo alumni
--     Exclude target alumni emails so ON CONFLICT below can handle them idempotently
DELETE FROM alumni_profile
WHERE user_id IN (
    SELECT id FROM users
    WHERE role_id = 1
      AND id NOT IN (112, 114, 120)
      AND email NOT IN (
          '202500719010175@glsu.edu.in',
          '202500719010165@glsu.edu.in',
          '202500719010174@glsu.edu.in',
          'harshposhiya@gmail.com',
          'sujalchovatiya@gmail.com',
          'devratvyas@gmail.com',
          'vrajpopat@gmail.com',
          'parthsoni@gmail.com',
          'aanshishah@gmail.com',
          'karanvisani@gmail.com',
          'dharmikbadheka@gmail.com',
          'dhruvprajapati@gmail.com',
          'devarshprajapati@gmail.com',
          'harshmakvana@gmail.com',
          'nischaymodi@gmail.com'
      )
);

-- 1e. Delete email_verification rows for old demo alumni emails
--     Exclude target alumni emails
DELETE FROM email_verification
WHERE email IN (
    SELECT email FROM users
    WHERE role_id = 1
      AND id NOT IN (112, 114, 120)
      AND email NOT IN (
          '202500719010175@glsu.edu.in',
          '202500719010165@glsu.edu.in',
          '202500719010174@glsu.edu.in',
          'harshposhiya@gmail.com',
          'sujalchovatiya@gmail.com',
          'devratvyas@gmail.com',
          'vrajpopat@gmail.com',
          'parthsoni@gmail.com',
          'aanshishah@gmail.com',
          'karanvisani@gmail.com',
          'dharmikbadheka@gmail.com',
          'dhruvprajapati@gmail.com',
          'devarshprajapati@gmail.com',
          'harshmakvana@gmail.com',
          'nischaymodi@gmail.com'
      )
);

-- 1f. Delete the old demo alumni User rows
--     Exclude target alumni emails so they remain on re-run
DELETE FROM users
WHERE role_id = 1
  AND id NOT IN (112, 114, 120)
  AND email NOT IN (
      '202500719010175@glsu.edu.in',
      '202500719010165@glsu.edu.in',
      '202500719010174@glsu.edu.in',
      'harshposhiya@gmail.com',
      'sujalchovatiya@gmail.com',
      'devratvyas@gmail.com',
      'vrajpopat@gmail.com',
      'parthsoni@gmail.com',
      'aanshishah@gmail.com',
      'karanvisani@gmail.com',
      'dharmikbadheka@gmail.com',
      'dhruvprajapati@gmail.com',
      'devarshprajapati@gmail.com',
      'harshmakvana@gmail.com',
      'nischaymodi@gmail.com'
  );

-- ---------------------------------------------------------------------------
-- SECTION 2: Remove old demo FACULTY records
--   Old faculty = ids 80-89 (Mahendra, Rajesh, Deepak, Sanjay, Manish, Paresh,
--                              Shailesh, Dinesh, Arvind, Hitesh)
--   Users 112 (vivekposhiya82@gmail.com) and 114 (vpposhiya@gmail.com) will be
--   repurposed — they are NOT in the faculty_profile table yet, so safe.
-- ---------------------------------------------------------------------------

-- 2a. Delete posts by old demo faculty
DELETE FROM posts
WHERE user_id IN (80, 81, 82, 83, 84, 85, 86, 87, 88, 89);

-- 2b. Delete jobs by old demo faculty
DELETE FROM jobs
WHERE user_id IN (80, 81, 82, 83, 84, 85, 86, 87, 88, 89);

-- 2c. Delete events by old demo faculty
DELETE FROM event
WHERE created_by IN (80, 81, 82, 83, 84, 85, 86, 87, 88, 89);

-- 2d. Delete faculty_profile for old demo faculty
DELETE FROM faculty_profile
WHERE user_id IN (80, 81, 82, 83, 84, 85, 86, 87, 88, 89);

-- 2e. Delete email_verification records for old demo faculty
DELETE FROM email_verification
WHERE email IN (
    SELECT email FROM users WHERE id IN (80,81,82,83,84,85,86,87,88,89)
);

-- 2f. Delete the old demo faculty User rows
DELETE FROM users
WHERE id IN (80, 81, 82, 83, 84, 85, 86, 87, 88, 89);

-- ---------------------------------------------------------------------------
-- SECTION 3: Re-purpose users 112 and 114 into FACULTY
--   User 112: vivekposhiya82@gmail.com → Prerna Agarwal (FACULTY)
--   User 114: vpposhiya@gmail.com      → Tushar Patel (FACULTY)
--   Both already have ALUMNI profiles — delete those first.
-- ---------------------------------------------------------------------------

-- 3a. Remove alumni_profile for users 112 and 114
DELETE FROM alumni_profile WHERE user_id IN (112, 114);

-- 3b. Remove any email_verification records for 112 and 114
DELETE FROM email_verification WHERE email IN (
    SELECT email FROM users WHERE id IN (112, 114)
);

-- 3c. Remove jobs/posts/events created by 112, 114 (they were demo alumni)
DELETE FROM jobs  WHERE user_id IN (112, 114);
DELETE FROM posts WHERE user_id IN (112, 114);
DELETE FROM event WHERE created_by IN (112, 114);

-- 3d. Update user 112 → Prerna Agarwal, FACULTY, verified
UPDATE users
SET name     = 'Prerna Agarwal',
    role_id  = 3,
    verified = TRUE
WHERE id = 112;

-- 3e. Update user 114 → Tushar Patel, FACULTY, verified
UPDATE users
SET name     = 'Tushar Patel',
    role_id  = 3,
    verified = TRUE
WHERE id = 114;

-- ---------------------------------------------------------------------------
-- SECTION 4: Fix user 120 (Vivek Poshiya, 202500719010175@glsu.edu.in)
--   Already alumni and verified. Update alumni_profile to correct values.
-- ---------------------------------------------------------------------------

UPDATE users
SET name     = 'Vivek Poshiya',
    verified = TRUE
WHERE id = 120;

-- Upsert alumni_profile for user 120
INSERT INTO alumni_profile (user_id, batch_year, degree, department)
VALUES (120, 2025, 'MCA', 'MCA')
ON CONFLICT (user_id)
DO UPDATE SET
    batch_year  = 2025,
    degree      = 'MCA',
    department  = 'MCA';

-- ---------------------------------------------------------------------------
-- SECTION 5: Insert 14 new alumni users
--   (Vivek Poshiya 202500719010175 at user 120 already exists)
--   We insert Jay Mistry (#2), Smit Patel (#3), and 12 name-based alumni.
--   Password: BCrypt hash of a placeholder locked password.
--   Using BCrypt hash of "SEEDED_LOCKED_ACCOUNT_NO_LOGIN" (not a real password):
--   $2a$10$XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
--   We generate a stable locked hash. Actual value set below.
--   NOTE: These accounts are NOT verified (verified=false) as they are seeded.
--         They are not activated. This is documented — see implementation report.
-- ---------------------------------------------------------------------------

-- Placeholder locked BCrypt hash (generated offline; not a guessable password)
-- BCrypt of random UUID string — accounts cannot be logged into without reset
-- Hash: $2a$10$8QvxvwUJPr1nXjnBcPz2Ue7.3pW5vvs0Eq8dKzCrX9GbbB0P3V5fy
-- (This is a valid BCrypt hash; the original plaintext is unknown/discarded)

DO $$
DECLARE
    v_locked_hash TEXT := '$2a$10$8QvxvwUJPr1nXjnBcPz2Ue7.3pW5vvs0Eq8dKzCrX9GbbB0P3V5fy';
    v_alumni_role_id INTEGER := 1;
BEGIN

    -- -------------------------------------------------------------------------
    -- 5.1  Jay Mistry — 202500719010165@glsu.edu.in
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Jay Mistry', '202500719010165@glsu.edu.in', v_locked_hash, v_alumni_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- 5.2  Smit Patel — 202500719010174@glsu.edu.in
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Smit Patel', '202500719010174@glsu.edu.in', v_locked_hash, v_alumni_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- 5.3  Harsh Poshiya
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Harsh Poshiya', 'harshposhiya@gmail.com', v_locked_hash, v_alumni_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- 5.4  Sujal Chovatiya
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Sujal Chovatiya', 'sujalchovatiya@gmail.com', v_locked_hash, v_alumni_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- 5.5  Devrat Vyas
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Devrat Vyas', 'devratvyas@gmail.com', v_locked_hash, v_alumni_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- 5.6  Vraj Popat
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Vraj Popat', 'vrajpopat@gmail.com', v_locked_hash, v_alumni_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- 5.7  Parth Soni
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Parth Soni', 'parthsoni@gmail.com', v_locked_hash, v_alumni_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- 5.8  Aanshi Shah
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Aanshi Shah', 'aanshishah@gmail.com', v_locked_hash, v_alumni_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- 5.9  Karan Visani
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Karan Visani', 'karanvisani@gmail.com', v_locked_hash, v_alumni_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- 5.10  Dharmik Badheka
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Dharmik Badheka', 'dharmikbadheka@gmail.com', v_locked_hash, v_alumni_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- 5.11  Dhruv Prajapati
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Dhruv Prajapati', 'dhruvprajapati@gmail.com', v_locked_hash, v_alumni_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- 5.12  Devarsh Prajapati
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Devarsh Prajapati', 'devarshprajapati@gmail.com', v_locked_hash, v_alumni_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- 5.13  Harsh Makvana
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Harsh Makvana', 'harshmakvana@gmail.com', v_locked_hash, v_alumni_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- 5.14  Nischay Modi
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Nischay Modi', 'nischaymodi@gmail.com', v_locked_hash, v_alumni_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

END $$;

-- ---------------------------------------------------------------------------
-- SECTION 6: Create alumni_profile records for all 15 alumni
-- ---------------------------------------------------------------------------

-- Drop unique constraint helper: alumni_profile.user_id must be unique (OneToOne)
-- The table has a unique constraint via JPA @OneToOne. We use ON CONFLICT (user_id).
-- First check the constraint name:

DO $$
DECLARE
    v_bio TEXT := 'MCA graduate (Batch 2025) from GLS University with a strong foundation in software development, databases, and web technologies.';
    v_skills TEXT := 'Java, Python, SQL, Web Development, Data Structures, Algorithms';
BEGIN

    -- Jay Mistry
    INSERT INTO alumni_profile (user_id, batch_year, degree, department)
    SELECT id, 2025, 'MCA', 'MCA' FROM users WHERE email = '202500719010165@glsu.edu.in'
    ON CONFLICT (user_id) DO UPDATE SET batch_year=2025, degree='MCA', department='MCA';

    -- Smit Patel
    INSERT INTO alumni_profile (user_id, batch_year, degree, department)
    SELECT id, 2025, 'MCA', 'MCA' FROM users WHERE email = '202500719010174@glsu.edu.in'
    ON CONFLICT (user_id) DO UPDATE SET batch_year=2025, degree='MCA', department='MCA';

    -- Harsh Poshiya
    INSERT INTO alumni_profile (user_id, batch_year, degree, department)
    SELECT id, 2025, 'MCA', 'MCA' FROM users WHERE email = 'harshposhiya@gmail.com'
    ON CONFLICT (user_id) DO UPDATE SET batch_year=2025, degree='MCA', department='MCA';

    -- Sujal Chovatiya
    INSERT INTO alumni_profile (user_id, batch_year, degree, department)
    SELECT id, 2025, 'MCA', 'MCA' FROM users WHERE email = 'sujalchovatiya@gmail.com'
    ON CONFLICT (user_id) DO UPDATE SET batch_year=2025, degree='MCA', department='MCA';

    -- Devrat Vyas
    INSERT INTO alumni_profile (user_id, batch_year, degree, department)
    SELECT id, 2025, 'MCA', 'MCA' FROM users WHERE email = 'devratvyas@gmail.com'
    ON CONFLICT (user_id) DO UPDATE SET batch_year=2025, degree='MCA', department='MCA';

    -- Vraj Popat
    INSERT INTO alumni_profile (user_id, batch_year, degree, department)
    SELECT id, 2025, 'MCA', 'MCA' FROM users WHERE email = 'vrajpopat@gmail.com'
    ON CONFLICT (user_id) DO UPDATE SET batch_year=2025, degree='MCA', department='MCA';

    -- Parth Soni
    INSERT INTO alumni_profile (user_id, batch_year, degree, department)
    SELECT id, 2025, 'MCA', 'MCA' FROM users WHERE email = 'parthsoni@gmail.com'
    ON CONFLICT (user_id) DO UPDATE SET batch_year=2025, degree='MCA', department='MCA';

    -- Aanshi Shah
    INSERT INTO alumni_profile (user_id, batch_year, degree, department)
    SELECT id, 2025, 'MCA', 'MCA' FROM users WHERE email = 'aanshishah@gmail.com'
    ON CONFLICT (user_id) DO UPDATE SET batch_year=2025, degree='MCA', department='MCA';

    -- Karan Visani
    INSERT INTO alumni_profile (user_id, batch_year, degree, department)
    SELECT id, 2025, 'MCA', 'MCA' FROM users WHERE email = 'karanvisani@gmail.com'
    ON CONFLICT (user_id) DO UPDATE SET batch_year=2025, degree='MCA', department='MCA';

    -- Dharmik Badheka
    INSERT INTO alumni_profile (user_id, batch_year, degree, department)
    SELECT id, 2025, 'MCA', 'MCA' FROM users WHERE email = 'dharmikbadheka@gmail.com'
    ON CONFLICT (user_id) DO UPDATE SET batch_year=2025, degree='MCA', department='MCA';

    -- Dhruv Prajapati
    INSERT INTO alumni_profile (user_id, batch_year, degree, department)
    SELECT id, 2025, 'MCA', 'MCA' FROM users WHERE email = 'dhruvprajapati@gmail.com'
    ON CONFLICT (user_id) DO UPDATE SET batch_year=2025, degree='MCA', department='MCA';

    -- Devarsh Prajapati
    INSERT INTO alumni_profile (user_id, batch_year, degree, department)
    SELECT id, 2025, 'MCA', 'MCA' FROM users WHERE email = 'devarshprajapati@gmail.com'
    ON CONFLICT (user_id) DO UPDATE SET batch_year=2025, degree='MCA', department='MCA';

    -- Harsh Makvana
    INSERT INTO alumni_profile (user_id, batch_year, degree, department)
    SELECT id, 2025, 'MCA', 'MCA' FROM users WHERE email = 'harshmakvana@gmail.com'
    ON CONFLICT (user_id) DO UPDATE SET batch_year=2025, degree='MCA', department='MCA';

    -- Nischay Modi
    INSERT INTO alumni_profile (user_id, batch_year, degree, department)
    SELECT id, 2025, 'MCA', 'MCA' FROM users WHERE email = 'nischaymodi@gmail.com'
    ON CONFLICT (user_id) DO UPDATE SET batch_year=2025, degree='MCA', department='MCA';

END $$;

-- ---------------------------------------------------------------------------
-- SECTION 7: Insert 9 new faculty users and their profiles
-- ---------------------------------------------------------------------------

DO $$
DECLARE
    v_locked_hash TEXT := '$2a$10$8QvxvwUJPr1nXjnBcPz2Ue7.3pW5vvs0Eq8dKzCrX9GbbB0P3V5fy';
    v_faculty_role_id INTEGER := 3;
BEGIN

    -- -------------------------------------------------------------------------
    -- Faculty 3: Devarshi Mehta — devarshi.mehta@glsuniversity.ac.in
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Devarshi Mehta', 'devarshi.mehta@glsuniversity.ac.in', v_locked_hash, v_faculty_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- Faculty 4: Krupa Mehta — krupa.mehta@glsuniversity.ac.in
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Krupa Mehta', 'krupa.mehta@glsuniversity.ac.in', v_locked_hash, v_faculty_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- Faculty 5: Arpit Jain — arpit.jain@glsuniversity.ac.in
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Arpit Jain', 'arpit.jain@glsuniversity.ac.in', v_locked_hash, v_faculty_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- Faculty 6: Girish Dave — girish.dave@glsuniversity.ac.in
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Girish Dave', 'girish.dave@glsuniversity.ac.in', v_locked_hash, v_faculty_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- Faculty 7: Darshita Kalyani — darshita.kalyani@glsuniversity.ac.in
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Darshita Kalyani', 'darshita.kalyani@glsuniversity.ac.in', v_locked_hash, v_faculty_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- Faculty 8: Meet Thakar — meet.thakar@glsuniversity.ac.in
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Meet Thakar', 'meet.thakar@glsuniversity.ac.in', v_locked_hash, v_faculty_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

    -- -------------------------------------------------------------------------
    -- Faculty 9: Jyotsna Khatri — jyotsna.khatri@glsuniversity.ac.in
    -- -------------------------------------------------------------------------
    INSERT INTO users (name, email, password, role_id, verified)
    VALUES ('Jyotsna Khatri', 'jyotsna.khatri@glsuniversity.ac.in', v_locked_hash, v_faculty_role_id, FALSE)
    ON CONFLICT (email) DO UPDATE
        SET name    = EXCLUDED.name,
            role_id = EXCLUDED.role_id;

END $$;

-- ---------------------------------------------------------------------------
-- SECTION 8: Create faculty_profile records for all 9 faculty members
-- ---------------------------------------------------------------------------

DO $$
BEGIN

    -- Prerna Agarwal (user 112, vivekposhiya82@gmail.com)
    INSERT INTO faculty_profile (user_id, department, specialization, skills, bio, email)
    SELECT id, 'MCA', 'Big Data Technologies, Machine Learning',
           'Big Data Technologies, Machine Learning',
           'Faculty member in the MCA department at GLS University with expertise in Big Data Technologies and Machine Learning.',
           'vivekposhiya82@gmail.com'
    FROM users WHERE id = 112
    ON CONFLICT (user_id) DO UPDATE
        SET department     = 'MCA',
            specialization = 'Big Data Technologies, Machine Learning',
            skills         = 'Big Data Technologies, Machine Learning',
            bio            = 'Faculty member in the MCA department at GLS University with expertise in Big Data Technologies and Machine Learning.',
            email          = 'vivekposhiya82@gmail.com';

    -- Tushar Patel (user 114, vpposhiya@gmail.com)
    INSERT INTO faculty_profile (user_id, department, bio, email)
    SELECT id, 'MCA',
           'Faculty member in the MCA department at GLS University.',
           'vpposhiya@gmail.com'
    FROM users WHERE id = 114
    ON CONFLICT (user_id) DO UPDATE
        SET department = 'MCA',
            bio        = 'Faculty member in the MCA department at GLS University.',
            email      = 'vpposhiya@gmail.com';

    -- Devarshi Mehta
    INSERT INTO faculty_profile (user_id, department, bio, email)
    SELECT id, 'MCA',
           'Faculty member in the MCA department at GLS University.',
           'devarshi.mehta@glsuniversity.ac.in'
    FROM users WHERE email = 'devarshi.mehta@glsuniversity.ac.in'
    ON CONFLICT (user_id) DO UPDATE
        SET department = 'MCA',
            bio        = 'Faculty member in the MCA department at GLS University.',
            email      = 'devarshi.mehta@glsuniversity.ac.in';

    -- Krupa Mehta
    INSERT INTO faculty_profile (user_id, department, bio, email)
    SELECT id, 'MCA',
           'Faculty member in the MCA department at GLS University.',
           'krupa.mehta@glsuniversity.ac.in'
    FROM users WHERE email = 'krupa.mehta@glsuniversity.ac.in'
    ON CONFLICT (user_id) DO UPDATE
        SET department = 'MCA',
            bio        = 'Faculty member in the MCA department at GLS University.',
            email      = 'krupa.mehta@glsuniversity.ac.in';

    -- Arpit Jain — Java Frameworks, Blockchain
    INSERT INTO faculty_profile (user_id, department, specialization, skills, bio, email)
    SELECT id, 'MCA', 'Java Frameworks, Blockchain',
           'Java Frameworks, Blockchain',
           'Faculty member in the MCA department at GLS University with expertise in Java Frameworks and Blockchain technologies.',
           'arpit.jain@glsuniversity.ac.in'
    FROM users WHERE email = 'arpit.jain@glsuniversity.ac.in'
    ON CONFLICT (user_id) DO UPDATE
        SET department     = 'MCA',
            specialization = 'Java Frameworks, Blockchain',
            skills         = 'Java Frameworks, Blockchain',
            bio            = 'Faculty member in the MCA department at GLS University with expertise in Java Frameworks and Blockchain technologies.',
            email          = 'arpit.jain@glsuniversity.ac.in';

    -- Girish Dave
    INSERT INTO faculty_profile (user_id, department, bio, email)
    SELECT id, 'MCA',
           'Faculty member in the MCA department at GLS University.',
           'girish.dave@glsuniversity.ac.in'
    FROM users WHERE email = 'girish.dave@glsuniversity.ac.in'
    ON CONFLICT (user_id) DO UPDATE
        SET department = 'MCA',
            bio        = 'Faculty member in the MCA department at GLS University.',
            email      = 'girish.dave@glsuniversity.ac.in';

    -- Darshita Kalyani
    INSERT INTO faculty_profile (user_id, department, bio, email)
    SELECT id, 'MCA',
           'Faculty member in the MCA department at GLS University.',
           'darshita.kalyani@glsuniversity.ac.in'
    FROM users WHERE email = 'darshita.kalyani@glsuniversity.ac.in'
    ON CONFLICT (user_id) DO UPDATE
        SET department = 'MCA',
            bio        = 'Faculty member in the MCA department at GLS University.',
            email      = 'darshita.kalyani@glsuniversity.ac.in';

    -- Meet Thakar — Flutter, Angular
    INSERT INTO faculty_profile (user_id, department, specialization, skills, bio, email)
    SELECT id, 'MCA', 'Flutter, Angular',
           'Flutter, Angular',
           'Faculty member in the MCA department at GLS University with expertise in Flutter and Angular frameworks.',
           'meet.thakar@glsuniversity.ac.in'
    FROM users WHERE email = 'meet.thakar@glsuniversity.ac.in'
    ON CONFLICT (user_id) DO UPDATE
        SET department     = 'MCA',
            specialization = 'Flutter, Angular',
            skills         = 'Flutter, Angular',
            bio            = 'Faculty member in the MCA department at GLS University with expertise in Flutter and Angular frameworks.',
            email          = 'meet.thakar@glsuniversity.ac.in';

    -- Jyotsna Khatri
    INSERT INTO faculty_profile (user_id, department, bio, email)
    SELECT id, 'MCA',
           'Faculty member in the MCA department at GLS University.',
           'jyotsna.khatri@glsuniversity.ac.in'
    FROM users WHERE email = 'jyotsna.khatri@glsuniversity.ac.in'
    ON CONFLICT (user_id) DO UPDATE
        SET department = 'MCA',
            bio        = 'Faculty member in the MCA department at GLS University.',
            email      = 'jyotsna.khatri@glsuniversity.ac.in';

END $$;

-- ---------------------------------------------------------------------------
-- SECTION 9: Verification queries
-- ---------------------------------------------------------------------------

SELECT 'ALUMNI COUNT' AS check_name, COUNT(*) AS result
FROM users WHERE role_id = 1;

SELECT 'FACULTY COUNT' AS check_name, COUNT(*) AS result
FROM users WHERE role_id = 3;

SELECT 'ALUMNI PROFILES COUNT' AS check_name, COUNT(*) AS result
FROM alumni_profile;

SELECT 'FACULTY PROFILES COUNT' AS check_name, COUNT(*) AS result
FROM faculty_profile;

-- Show all alumni
SELECT u.id, u.name, u.email, u.verified, ap.batch_year, ap.degree, ap.department
FROM users u
JOIN alumni_profile ap ON ap.user_id = u.id
WHERE u.role_id = 1
ORDER BY u.id;

-- Show all faculty
SELECT u.id, u.name, u.email, u.verified, fp.department, fp.specialization, fp.skills
FROM users u
JOIN faculty_profile fp ON fp.user_id = u.id
WHERE u.role_id = 3
ORDER BY u.id;

COMMIT;
