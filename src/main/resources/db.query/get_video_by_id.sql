SELECT v.id, m.nickname, v.created_at, v.running_time
FROM video as v
LEFT JOIN member as m ON v.member_id = m.id
WHERE m.id != '73470cbc-b383-4d92-8d74-fd68391de431'
AND m.id != 'f0e9550d-d665-41d3-a686-3750fbd58a99'
AND m.id != '8f57fffc-7ccb-4763-abe2-39456278185b'
AND m.id != '99fb57cc-96a9-4db6-a4c8-de1d6b0b58e1'
AND m.id != '9c1050c4-5238-419f-8718-959f8ddfcd4a'
AND m.id != '7872de70-9bed-45a1-80f3-ea9666ba87f3'
