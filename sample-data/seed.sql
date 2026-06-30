-- ============================================================================
-- Smart Lab — Sample Data
-- ----------------------------------------------------------------------------
-- Seeds 14 sample users (HoDs, Lecturers, Instructors, Students), 4 labs,
-- 16 items (10 borrowable + 6 lab-only), 7 bookings (one in each lifecycle
-- state), and a handful of notifications.
--
-- How to run:
--   1) Make sure all 4 services have started at least once so Flyway has
--      created the schemas (users, equipment, bookings, notifications) and
--      SystemBootstrap has seeded the Main Admin + 4 Department Admins.
--   2) Open Supabase → SQL Editor (or run `psql -f seed.sql`).
--   3) Paste this file. Execute.
--
-- All sample passwords are: Password1
--
-- Idempotent: this file deletes any existing sample rows before re-inserting,
-- so it's safe to run multiple times. It does NOT touch admins, faculties,
-- departments, or any non-sample data.
-- ============================================================================


-- ============================================================================
-- 0. Wipe previous sample rows (in FK-safe order)
-- ============================================================================

DELETE FROM notifications.notifications WHERE title LIKE 'SAMPLE %';

DELETE FROM bookings.booking_attachments WHERE booking_id IN (
  SELECT id FROM bookings.bookings WHERE project_name LIKE 'SAMPLE %'
);
DELETE FROM bookings.booking_events WHERE booking_id IN (
  SELECT id FROM bookings.bookings WHERE project_name LIKE 'SAMPLE %'
);
DELETE FROM bookings.booking_items WHERE booking_id IN (
  SELECT id FROM bookings.bookings WHERE project_name LIKE 'SAMPLE %'
);
DELETE FROM bookings.bookings WHERE project_name LIKE 'SAMPLE %';

DELETE FROM equipment.items WHERE serial_number LIKE 'SAMPLE-%';
DELETE FROM equipment.labs  WHERE name LIKE 'Sample %';

DELETE FROM users.users WHERE email IN (
  'hod.computer@foe.sjp.ac.lk', 'hod.electrical@foe.sjp.ac.lk',
  'lec.computer@foe.sjp.ac.lk', 'lec.mechanical@foe.sjp.ac.lk',
  'inst.computer@foe.sjp.ac.lk', 'inst.electrical@foe.sjp.ac.lk',
  'inst.mechanical@foe.sjp.ac.lk', 'inst.civil@foe.sjp.ac.lk',
  'student1@stu.sjp.ac.lk', 'student2@stu.sjp.ac.lk', 'student3@stu.sjp.ac.lk',
  'student4@stu.sjp.ac.lk', 'student5@stu.sjp.ac.lk', 'student6@stu.sjp.ac.lk'
);


-- ============================================================================
-- 1. Sample users — password for ALL of them is "Password1"
--    bcrypt $2y$10$... format; Spring's BCryptPasswordEncoder accepts it.
-- ============================================================================

INSERT INTO users.users
  (email, password, full_name, role, status, faculty_id, department_id, phone_number,
   en_number, index_number, name_with_initial, uni_email)
SELECT v.email, v.password, v.full_name, v.role, 'ACTIVE',
       (SELECT id FROM users.faculties WHERE code = 'FOE'),
       (SELECT id FROM users.departments WHERE code = v.dept_code),
       v.phone, v.en_number, v.index_number, v.name_with_initial, v.uni_email
FROM (VALUES
  -- ===== Heads of Department (HoDs) =====
  ('hod.computer@foe.sjp.ac.lk',
   '$2y$10$feh1KqsMw.bYEW9Zk1UuxO5AK9I/d/Y0mAO4vCRB0ls/ffRKCfnI6',
   'Dr. Saman Perera',          'HOD',        'CE',  '0712345101', NULL, NULL, NULL, NULL),
  ('hod.electrical@foe.sjp.ac.lk',
   '$2y$10$feh1KqsMw.bYEW9Zk1UuxO5AK9I/d/Y0mAO4vCRB0ls/ffRKCfnI6',
   'Dr. Nadeesha Fernando',     'HOD',        'EE',  '0712345102', NULL, NULL, NULL, NULL),

  -- ===== Lecturers =====
  ('lec.computer@foe.sjp.ac.lk',
   '$2y$10$feh1KqsMw.bYEW9Zk1UuxO5AK9I/d/Y0mAO4vCRB0ls/ffRKCfnI6',
   'Dr. Lakshani Silva',        'LECTURER',   'CE',  '0712345201', NULL, NULL, NULL, NULL),
  ('lec.mechanical@foe.sjp.ac.lk',
   '$2y$10$feh1KqsMw.bYEW9Zk1UuxO5AK9I/d/Y0mAO4vCRB0ls/ffRKCfnI6',
   'Dr. Asanka Rajapaksa',      'LECTURER',   'ME',  '0712345202', NULL, NULL, NULL, NULL),

  -- ===== Instructors (one per department, pre-approved) =====
  ('inst.computer@foe.sjp.ac.lk',
   '$2y$10$feh1KqsMw.bYEW9Zk1UuxO5AK9I/d/Y0mAO4vCRB0ls/ffRKCfnI6',
   'Mr. Chamara Bandara',       'INSTRUCTOR', 'CE',  '0712345301', NULL, NULL, NULL, NULL),
  ('inst.electrical@foe.sjp.ac.lk',
   '$2y$10$feh1KqsMw.bYEW9Zk1UuxO5AK9I/d/Y0mAO4vCRB0ls/ffRKCfnI6',
   'Ms. Tharushi Wickramasinghe', 'INSTRUCTOR', 'EE',  '0712345302', NULL, NULL, NULL, NULL),
  ('inst.mechanical@foe.sjp.ac.lk',
   '$2y$10$feh1KqsMw.bYEW9Zk1UuxO5AK9I/d/Y0mAO4vCRB0ls/ffRKCfnI6',
   'Mr. Rohitha Jayasinghe',    'INSTRUCTOR', 'ME',  '0712345303', NULL, NULL, NULL, NULL),
  ('inst.civil@foe.sjp.ac.lk',
   '$2y$10$feh1KqsMw.bYEW9Zk1UuxO5AK9I/d/Y0mAO4vCRB0ls/ffRKCfnI6',
   'Mr. Dulan Karunaratne',     'INSTRUCTOR', 'CIV', '0712345304', NULL, NULL, NULL, NULL),

  -- ===== Students =====
  ('student1@stu.sjp.ac.lk',
   '$2y$10$feh1KqsMw.bYEW9Zk1UuxO5AK9I/d/Y0mAO4vCRB0ls/ffRKCfnI6',
   'Hashini Madushani',         'STUDENT',    'CE',  '0772341001',
   'EN/2021/0001', 'CS/21/001', 'H.M. Madushani', 'hashini@stu.sjp.ac.lk'),
  ('student2@stu.sjp.ac.lk',
   '$2y$10$feh1KqsMw.bYEW9Zk1UuxO5AK9I/d/Y0mAO4vCRB0ls/ffRKCfnI6',
   'Pasindu Senanayake',        'STUDENT',    'CE',  '0772341002',
   'EN/2021/0002', 'CS/21/002', 'P. Senanayake',  'pasindu@stu.sjp.ac.lk'),
  ('student3@stu.sjp.ac.lk',
   '$2y$10$feh1KqsMw.bYEW9Zk1UuxO5AK9I/d/Y0mAO4vCRB0ls/ffRKCfnI6',
   'Dilini Wijesekara',         'STUDENT',    'EE',  '0772341003',
   'EN/2021/0003', 'EE/21/001', 'D.W. Dilini',    'dilini@stu.sjp.ac.lk'),
  ('student4@stu.sjp.ac.lk',
   '$2y$10$feh1KqsMw.bYEW9Zk1UuxO5AK9I/d/Y0mAO4vCRB0ls/ffRKCfnI6',
   'Tharindu Mendis',           'STUDENT',    'EE',  '0772341004',
   'EN/2021/0004', 'EE/21/002', 'T. Mendis',      'tharindu@stu.sjp.ac.lk'),
  ('student5@stu.sjp.ac.lk',
   '$2y$10$feh1KqsMw.bYEW9Zk1UuxO5AK9I/d/Y0mAO4vCRB0ls/ffRKCfnI6',
   'Janith Gunasekara',         'STUDENT',    'ME',  '0772341005',
   'EN/2021/0005', 'ME/21/001', 'J. Gunasekara',  'janith@stu.sjp.ac.lk'),
  ('student6@stu.sjp.ac.lk',
   '$2y$10$feh1KqsMw.bYEW9Zk1UuxO5AK9I/d/Y0mAO4vCRB0ls/ffRKCfnI6',
   'Sachini Hettiarachchi',     'STUDENT',    'CIV', '0772341006',
   'EN/2021/0006', 'CIV/21/001', 'S. Hettiarachchi', 'sachini@stu.sjp.ac.lk')
) AS v(email, password, full_name, role, dept_code, phone,
       en_number, index_number, name_with_initial, uni_email);


-- ============================================================================
-- 2. Sample labs — one per department, each owned by the matching instructor
-- ============================================================================

INSERT INTO equipment.labs (department_id, name, location, description, instructor_user_id)
SELECT (SELECT id FROM users.departments WHERE code = v.dept_code),
       v.name, v.location, v.description,
       (SELECT id FROM users.users WHERE email = v.instructor_email)
FROM (VALUES
  ('CE',  'Sample Computer Lab A',     'Engineering Building · Floor 2',
   'General-purpose computing and embedded systems lab.',
   'inst.computer@foe.sjp.ac.lk'),
  ('EE',  'Sample Electronics Lab B',  'Engineering Building · Floor 1',
   'Test & measurement, signal generation, and analogue prototyping.',
   'inst.electrical@foe.sjp.ac.lk'),
  ('ME',  'Sample Mechanical Workshop','Workshop Block · Bay 3',
   'Additive manufacturing, CNC, and laser cutting.',
   'inst.mechanical@foe.sjp.ac.lk'),
  ('CIV', 'Sample Civil Materials Lab','Materials Block · Room M-12',
   'Concrete, soil, and structural materials testing.',
   'inst.civil@foe.sjp.ac.lk')
) AS v(dept_code, name, location, description, instructor_email);


-- ============================================================================
-- 3. Sample items — mix of statuses across the four labs
-- ============================================================================

INSERT INTO equipment.items
  (lab_id, model, name, category, serial_number, status, description, usage_type)
SELECT (SELECT id FROM equipment.labs WHERE name = v.lab_name),
       v.model, v.name, v.category, v.serial, v.status, v.description, v.usage_type
FROM (VALUES
  -- Sample Computer Lab A
  ('Sample Computer Lab A', 'Raspberry Pi 5 8GB',  'Raspberry Pi Kit #1', 'Computing',    'SAMPLE-RPI-01', 'AVAILABLE',   'RPi 5 + camera + sensor pack',          'BORROWABLE'),
  ('Sample Computer Lab A', 'Raspberry Pi 5 8GB',  'Raspberry Pi Kit #2', 'Computing',    'SAMPLE-RPI-02', 'IN_USE',      'RPi 5 + camera + sensor pack',          'BORROWABLE'),
  ('Sample Computer Lab A', 'NVIDIA Jetson Nano',  'Jetson Nano Devkit',  'Computing',    'SAMPLE-JTN-01', 'AVAILABLE',   'For ML / vision projects — used in the lab', 'LAB_ONLY'),
  ('Sample Computer Lab A', 'Arduino Uno R4',      'Arduino Starter Kit', 'Electronics',  'SAMPLE-ARD-01', 'AVAILABLE',   'Uno R4 with sensor shield — used in the lab', 'LAB_ONLY'),
  -- Lab-only: a heavy GPU workstation used in the lab at a confirmed time
  ('Sample Computer Lab A', 'Dell Precision 7960', 'GPU Workstation',     'Computing',    'SAMPLE-WGPU-01','AVAILABLE',   'Dual RTX 6000 — train models in the lab','LAB_ONLY'),

  -- Sample Electronics Lab B
  ('Sample Electronics Lab B', 'Tektronix TBS1052B', 'Oscilloscope #1',     'Electronics', 'SAMPLE-OSC-01', 'AVAILABLE',   '50 MHz, 1 GS/s, two channels',          'BORROWABLE'),
  ('Sample Electronics Lab B', 'Tektronix TBS1052B', 'Oscilloscope #2',     'Electronics', 'SAMPLE-OSC-02', 'MAINTENANCE', 'Channel 2 calibration drift',           'BORROWABLE'),
  ('Sample Electronics Lab B', 'Rigol DG1022',       'Function Generator',  'Electronics', 'SAMPLE-FNG-01', 'AVAILABLE',   '20 MHz arbitrary / function generator', 'BORROWABLE'),
  ('Sample Electronics Lab B', 'NI USB-6009',        'DAQ Module',          'Electronics', 'SAMPLE-DAQ-01', 'AVAILABLE',   'Multifunction DAQ, 14-bit',             'BORROWABLE'),
  -- Lab-only: a benchtop spectrum analyzer that stays in the lab
  ('Sample Electronics Lab B', 'Keysight N9320B',    'Spectrum Analyzer',   'Electronics', 'SAMPLE-SPA-01', 'AVAILABLE',   '9 kHz–3 GHz — bench use in the EE lab', 'LAB_ONLY'),

  -- Sample Mechanical Workshop
  ('Sample Mechanical Workshop', 'Prusa MK4',     '3D Printer #1',           'Mechanical',    'SAMPLE-PRT-01', 'AVAILABLE',   'PLA / PETG, 250x210x220 mm bed',     'BORROWABLE'),
  ('Sample Mechanical Workshop', 'Glowforge Pro', 'Laser Cutter',            'Manufacturing', 'SAMPLE-LSR-01', 'AVAILABLE',   '45W laser, 28x36 cm bed',            'BORROWABLE'),
  ('Sample Mechanical Workshop', 'Tormach 770M',  'CNC Mill',                'Manufacturing', 'SAMPLE-CNC-01', 'OUT_OF_SERVICE', 'Spindle motor failure — repair pending', 'BORROWABLE'),
  -- Lab-only: CNC lathe operated only under supervision in the workshop
  ('Sample Mechanical Workshop', 'Haas TL-1',     'CNC Lathe Station',       'Manufacturing', 'SAMPLE-LTH-01', 'AVAILABLE',   'Supervised use only — book a workshop slot', 'LAB_ONLY'),

  -- Sample Civil Materials Lab
  ('Sample Civil Materials Lab', 'Instron 5985',   'Universal Testing Machine','Civil',     'SAMPLE-UTM-01', 'AVAILABLE',   'Up to 250 kN tension/compression',   'BORROWABLE'),
  -- Lab-only: compression tester fixed to the lab floor
  ('Sample Civil Materials Lab', 'ELE ADR-Auto',   'Concrete Compression Tester','Civil',  'SAMPLE-CCT-01', 'AVAILABLE',   '2000 kN — used in the materials lab', 'LAB_ONLY')
) AS v(lab_name, model, name, category, serial, status, description, usage_type);


-- ============================================================================
-- 4. Sample bookings — one in each lifecycle state
-- ============================================================================

DO $$
DECLARE
  v_student1 BIGINT := (SELECT id FROM users.users WHERE email = 'student1@stu.sjp.ac.lk');
  v_student2 BIGINT := (SELECT id FROM users.users WHERE email = 'student2@stu.sjp.ac.lk');
  v_student3 BIGINT := (SELECT id FROM users.users WHERE email = 'student3@stu.sjp.ac.lk');
  v_student4 BIGINT := (SELECT id FROM users.users WHERE email = 'student4@stu.sjp.ac.lk');
  v_student5 BIGINT := (SELECT id FROM users.users WHERE email = 'student5@stu.sjp.ac.lk');
  v_student6 BIGINT := (SELECT id FROM users.users WHERE email = 'student6@stu.sjp.ac.lk');

  v_dept_ce  BIGINT := (SELECT id FROM users.departments WHERE code = 'CE');
  v_dept_ee  BIGINT := (SELECT id FROM users.departments WHERE code = 'EE');
  v_dept_me  BIGINT := (SELECT id FROM users.departments WHERE code = 'ME');
  v_dept_civ BIGINT := (SELECT id FROM users.departments WHERE code = 'CIV');

  v_inst_ce  BIGINT := (SELECT id FROM users.users WHERE email = 'inst.computer@foe.sjp.ac.lk');
  v_inst_ee  BIGINT := (SELECT id FROM users.users WHERE email = 'inst.electrical@foe.sjp.ac.lk');
  v_inst_me  BIGINT := (SELECT id FROM users.users WHERE email = 'inst.mechanical@foe.sjp.ac.lk');
  v_inst_civ BIGINT := (SELECT id FROM users.users WHERE email = 'inst.civil@foe.sjp.ac.lk');

  v_hod_ce   BIGINT := (SELECT id FROM users.users WHERE email = 'hod.computer@foe.sjp.ac.lk');
  v_hod_ee   BIGINT := (SELECT id FROM users.users WHERE email = 'hod.electrical@foe.sjp.ac.lk');

  v_lab_ce   BIGINT := (SELECT id FROM equipment.labs WHERE name = 'Sample Computer Lab A');
  v_lab_ee   BIGINT := (SELECT id FROM equipment.labs WHERE name = 'Sample Electronics Lab B');
  v_lab_me   BIGINT := (SELECT id FROM equipment.labs WHERE name = 'Sample Mechanical Workshop');
  v_lab_civ  BIGINT := (SELECT id FROM equipment.labs WHERE name = 'Sample Civil Materials Lab');

  v_item_rpi BIGINT := (SELECT id FROM equipment.items WHERE serial_number = 'SAMPLE-RPI-01');
  v_item_jtn BIGINT := (SELECT id FROM equipment.items WHERE serial_number = 'SAMPLE-JTN-01');
  v_item_osc BIGINT := (SELECT id FROM equipment.items WHERE serial_number = 'SAMPLE-OSC-01');
  v_item_fng BIGINT := (SELECT id FROM equipment.items WHERE serial_number = 'SAMPLE-FNG-01');
  v_item_daq BIGINT := (SELECT id FROM equipment.items WHERE serial_number = 'SAMPLE-DAQ-01');
  v_item_prt BIGINT := (SELECT id FROM equipment.items WHERE serial_number = 'SAMPLE-PRT-01');
  v_item_lsr BIGINT := (SELECT id FROM equipment.items WHERE serial_number = 'SAMPLE-LSR-01');
  v_item_utm BIGINT := (SELECT id FROM equipment.items WHERE serial_number = 'SAMPLE-UTM-01');

  v_booking_id BIGINT;
  v_line_id    BIGINT;
BEGIN

  -- ===== 1. SUBMITTED — student1 just filed a request =====
  INSERT INTO bookings.bookings
    (student_user_id, student_department_id, project_name, purpose, start_date, return_date, state)
  VALUES (v_student1, v_dept_ce, 'SAMPLE 1: Wireless sensor prototype',
          'Senior project bench tests — needs Pi + camera kit.',
          NOW() + INTERVAL '2 days', NOW() + INTERVAL '5 days', 'SUBMITTED')
  RETURNING id INTO v_booking_id;

  INSERT INTO bookings.booking_items
    (booking_id, item_id, lab_id, instructor_user_id, state, last_actor_user_id)
  VALUES (v_booking_id, v_item_rpi, v_lab_ce, v_inst_ce, 'SUBMITTED', v_student1)
  RETURNING id INTO v_line_id;

  INSERT INTO bookings.booking_events (booking_id, booking_item_id, actor_user_id, to_state, note)
  VALUES (v_booking_id, v_line_id, v_student1, 'SUBMITTED', 'Submitted with 1 item');


  -- ===== 2. INSTRUCTOR_REVIEWING — student2 picked the laser cutter =====
  INSERT INTO bookings.bookings
    (student_user_id, student_department_id, project_name, purpose, start_date, return_date, state)
  VALUES (v_student2, v_dept_ce, 'SAMPLE 2: Laser-cut acrylic enclosure',
          'Cutting a custom enclosure for the wireless sensor project.',
          NOW() + INTERVAL '3 days', NOW() + INTERVAL '4 days', 'INSTRUCTOR_REVIEWING')
  RETURNING id INTO v_booking_id;

  INSERT INTO bookings.booking_items
    (booking_id, item_id, lab_id, instructor_user_id, state, last_actor_user_id)
  VALUES (v_booking_id, v_item_lsr, v_lab_me, v_inst_me, 'INSTRUCTOR_REVIEWING', v_inst_me)
  RETURNING id INTO v_line_id;

  INSERT INTO bookings.booking_events (booking_id, booking_item_id, actor_user_id, from_state, to_state, note)
  VALUES
    (v_booking_id, v_line_id, v_student2, NULL,        'SUBMITTED',            'Submitted with 1 item'),
    (v_booking_id, v_line_id, v_inst_me,  'SUBMITTED', 'INSTRUCTOR_REVIEWING', 'Picked up by instructor');


  -- ===== 3. AWAITING_SUPERVISOR — student3, EE oscilloscope, delegated to HoD-EE =====
  INSERT INTO bookings.bookings
    (student_user_id, student_department_id, project_name, purpose, start_date, return_date,
     nominated_supervisor_user_id, state)
  VALUES (v_student3, v_dept_ee, 'SAMPLE 3: Audio amplifier characterisation',
          'Frequency response analysis of a class-D amplifier.',
          NOW() + INTERVAL '5 days', NOW() + INTERVAL '12 days',
          v_hod_ee, 'AWAITING_SUPERVISOR')
  RETURNING id INTO v_booking_id;

  INSERT INTO bookings.booking_items
    (booking_id, item_id, lab_id, instructor_user_id, assigned_supervisor_user_id, state, last_actor_user_id)
  VALUES (v_booking_id, v_item_osc, v_lab_ee, v_inst_ee, v_hod_ee, 'AWAITING_SUPERVISOR', v_inst_ee)
  RETURNING id INTO v_line_id;

  INSERT INTO bookings.booking_events (booking_id, booking_item_id, actor_user_id, from_state, to_state, note)
  VALUES
    (v_booking_id, v_line_id, v_student3, NULL,                   'SUBMITTED',            NULL),
    (v_booking_id, v_line_id, v_inst_ee,  'SUBMITTED',            'INSTRUCTOR_REVIEWING', NULL),
    (v_booking_id, v_line_id, v_inst_ee,  'INSTRUCTOR_REVIEWING', 'AWAITING_SUPERVISOR',  'Delegated to HoD-EE for sign-off');


  -- ===== 4. READY_FOR_COLLECTION — student4, DAQ module approved =====
  INSERT INTO bookings.bookings
    (student_user_id, student_department_id, project_name, purpose, start_date, return_date, state)
  VALUES (v_student4, v_dept_ee, 'SAMPLE 4: Power-monitoring DAQ rig',
          'Logging current draw of the prototype motor controller.',
          NOW() + INTERVAL '1 day', NOW() + INTERVAL '8 days', 'READY_FOR_COLLECTION')
  RETURNING id INTO v_booking_id;

  INSERT INTO bookings.booking_items
    (booking_id, item_id, lab_id, instructor_user_id, state, pickup_at, pickup_note, last_actor_user_id)
  VALUES (v_booking_id, v_item_daq, v_lab_ee, v_inst_ee, 'READY_FOR_COLLECTION',
          NOW() + INTERVAL '1 day' + INTERVAL '4 hours',
          'Bring student ID and the project brief — pickup at the EE lab counter.',
          v_inst_ee)
  RETURNING id INTO v_line_id;

  INSERT INTO bookings.booking_events (booking_id, booking_item_id, actor_user_id, from_state, to_state, note)
  VALUES
    (v_booking_id, v_line_id, v_student4, NULL,                   'SUBMITTED',            NULL),
    (v_booking_id, v_line_id, v_inst_ee,  'SUBMITTED',            'INSTRUCTOR_REVIEWING', NULL),
    (v_booking_id, v_line_id, v_inst_ee,  'INSTRUCTOR_REVIEWING', 'READY_FOR_COLLECTION', 'Approved directly by instructor');


  -- ===== 5. COLLECTED — student5, 3D printer in active use =====
  INSERT INTO bookings.bookings
    (student_user_id, student_department_id, project_name, purpose, start_date, return_date, state)
  VALUES (v_student5, v_dept_me, 'SAMPLE 5: Drone chassis printing run',
          '24h continuous PETG print of a drone chassis prototype.',
          NOW() - INTERVAL '1 day', NOW() + INTERVAL '2 days', 'COLLECTED')
  RETURNING id INTO v_booking_id;

  INSERT INTO bookings.booking_items
    (booking_id, item_id, lab_id, instructor_user_id, state, pickup_at, pickup_note, last_actor_user_id)
  VALUES (v_booking_id, v_item_prt, v_lab_me, v_inst_me, 'COLLECTED',
          NOW() - INTERVAL '1 day',
          'Filament left in machine. Clean nozzle before return.',
          v_inst_me)
  RETURNING id INTO v_line_id;

  INSERT INTO bookings.booking_events (booking_id, booking_item_id, actor_user_id, from_state, to_state, note)
  VALUES
    (v_booking_id, v_line_id, v_student5, NULL,                   'SUBMITTED',            NULL),
    (v_booking_id, v_line_id, v_inst_me,  'SUBMITTED',            'INSTRUCTOR_REVIEWING', NULL),
    (v_booking_id, v_line_id, v_inst_me,  'INSTRUCTOR_REVIEWING', 'READY_FOR_COLLECTION', NULL),
    (v_booking_id, v_line_id, v_inst_me,  'READY_FOR_COLLECTION', 'COLLECTED',            'Picked up; filament loaded');


  -- ===== 6. RETURNED — student6, UTM completed cycle =====
  INSERT INTO bookings.bookings
    (student_user_id, student_department_id, project_name, purpose, start_date, return_date, state)
  VALUES (v_student6, v_dept_civ, 'SAMPLE 6: Concrete cube compression test',
          '28-day strength test on three sample cubes.',
          NOW() - INTERVAL '8 days', NOW() - INTERVAL '1 day', 'COMPLETED')
  RETURNING id INTO v_booking_id;

  INSERT INTO bookings.booking_items
    (booking_id, item_id, lab_id, instructor_user_id, state, pickup_at, last_actor_user_id)
  VALUES (v_booking_id, v_item_utm, v_lab_civ, v_inst_civ, 'RETURNED',
          NOW() - INTERVAL '8 days',
          v_inst_civ)
  RETURNING id INTO v_line_id;

  INSERT INTO bookings.booking_events (booking_id, booking_item_id, actor_user_id, from_state, to_state, note)
  VALUES
    (v_booking_id, v_line_id, v_student6, NULL,                   'SUBMITTED',            NULL),
    (v_booking_id, v_line_id, v_inst_civ, 'SUBMITTED',            'INSTRUCTOR_REVIEWING', NULL),
    (v_booking_id, v_line_id, v_inst_civ, 'INSTRUCTOR_REVIEWING', 'READY_FOR_COLLECTION', NULL),
    (v_booking_id, v_line_id, v_inst_civ, 'READY_FOR_COLLECTION', 'COLLECTED',            NULL),
    (v_booking_id, v_line_id, v_inst_civ, 'COLLECTED',            'RETURNED',             'Returned in good condition');


  -- ===== 7. INSTRUCTOR_REJECTED — student1, rejected (date conflict) =====
  INSERT INTO bookings.bookings
    (student_user_id, student_department_id, project_name, purpose, start_date, return_date, state)
  VALUES (v_student1, v_dept_ce, 'SAMPLE 7: Spectrum analysis sweep',
          'Frequency sweep for FYP — needed function generator.',
          NOW() + INTERVAL '4 days', NOW() + INTERVAL '6 days', 'INSTRUCTOR_REJECTED')
  RETURNING id INTO v_booking_id;

  INSERT INTO bookings.booking_items
    (booking_id, item_id, lab_id, instructor_user_id, state, last_actor_user_id)
  VALUES (v_booking_id, v_item_fng, v_lab_ee, v_inst_ee, 'INSTRUCTOR_REJECTED', v_inst_ee)
  RETURNING id INTO v_line_id;

  INSERT INTO bookings.booking_events (booking_id, booking_item_id, actor_user_id, from_state, to_state, note)
  VALUES
    (v_booking_id, v_line_id, v_student1, NULL,        'SUBMITTED',           NULL),
    (v_booking_id, v_line_id, v_inst_ee,  'SUBMITTED', 'INSTRUCTOR_REJECTED', 'Conflicts with scheduled lab class — pick a different week');

END $$;


-- ============================================================================
-- 5. Sample notifications — one per booking state, plus an instructor-account-approved notice
-- ============================================================================

INSERT INTO notifications.notifications (user_id, title, message, type, is_read, created_at)
SELECT (SELECT id FROM users.users WHERE email = v.recipient_email),
       v.title, v.message, v.type, v.is_read, v.created_at
FROM (VALUES
  ('inst.computer@foe.sjp.ac.lk',  'SAMPLE New booking awaiting your review',
   'Hashini Madushani requested Raspberry Pi Kit #1 from Sample Computer Lab A · SAMPLE 1: Wireless sensor prototype.',
   'BOOKING_NEEDS_REVIEW', false, NOW() - INTERVAL '2 hours'),

  ('student1@stu.sjp.ac.lk',       'SAMPLE Booking submitted',
   'Your booking #1 (1 item) is awaiting instructor review.',
   'BOOKING_SUBMITTED', false, NOW() - INTERVAL '2 hours'),

  ('hod.electrical@foe.sjp.ac.lk', 'SAMPLE Booking awaiting your approval',
   'Instructor forwarded a line of booking #3 for your sign-off: Dilini Wijesekara requested Oscilloscope #1 from Sample Electronics Lab B.',
   'BOOKING_NEEDS_SUPERVISOR_APPROVAL', false, NOW() - INTERVAL '6 hours'),

  ('student4@stu.sjp.ac.lk',       'SAMPLE Booking line approved — ready for collection',
   'Your booking #4 line for DAQ Module is approved. Collect it tomorrow afternoon at the EE lab counter.',
   'BOOKING_APPROVED', true, NOW() - INTERVAL '1 day'),

  ('inst.mechanical@foe.sjp.ac.lk','SAMPLE Booking line now overdue',
   'Booking #5 — Janith Gunasekara has not returned 3D Printer #1 (due tomorrow).',
   'BOOKING_OVERDUE', false, NOW() - INTERVAL '3 hours'),

  ('student1@stu.sjp.ac.lk',       'SAMPLE Booking line rejected',
   'Your booking #7 line for Function Generator was rejected. Reason: Conflicts with scheduled lab class — pick a different week.',
   'BOOKING_REJECTED', false, NOW() - INTERVAL '4 hours'),

  ('inst.civil@foe.sjp.ac.lk',     'SAMPLE Instructor account approved',
   'Your account has been approved by admin. You can now log in.',
   'INSTRUCTOR_ACCOUNT_APPROVED', true, NOW() - INTERVAL '7 days')
) AS v(recipient_email, title, message, type, is_read, created_at);


-- ============================================================================
-- Done. Quick sanity check: counts you should see.
-- ============================================================================
--   users.users         : +14 sample rows  (plus your existing admins)
--   equipment.labs      : 4
--   equipment.items     : 16  (10 borrowable + 6 lab-only)
--   bookings.bookings   : 7
--   bookings.booking_items : 7
--   bookings.booking_events : 28
--   notifications.notifications : 7
-- ============================================================================
