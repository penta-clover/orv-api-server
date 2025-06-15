EXPLAIN ANALYSE
SELECT t.id,
       t.name,
       t.description,
       t.thumbnail_url,
       COALESCE(
               (json_agg(
                DISTINCT json_build_object('name', h.name, 'color', h.color)::jsonb
                        ) FILTER (WHERE h.id IS NOT NULL)) ::text,
               '[]'
       ) ::json AS hashtags
FROM topic t
         JOIN category_topic ct ON t.id = ct.topic_id
         JOIN category c ON ct.category_id = c.id
         LEFT JOIN hashtag_topic ht ON t.id = ht.topic_id
         LEFT JOIN hashtag h ON h.id = ht.hashtag_id
WHERE c.code = 'DEFAULT'
GROUP BY t.id, t.name, t.description, t.thumbnail_url;

DISCARD PLANS;



EXPLAIN ANALYSE
WITH category_topics AS (SELECT t.id, t.name, t.description, t.thumbnail_url
                         FROM category c
                                  JOIN category_topic ct ON ct.category_id = c.id
                                  JOIN topic t ON t.id = ct.topic_id
                         WHERE c.code = 'DEFAULT' -- 먼저 필터
)
SELECT ct.id,
       ct.name,
       ct.description,
       ct.thumbnail_url,
       COALESCE(hs.hashtags, '[]'::json) AS hashtags
FROM category_topics ct
         LEFT JOIN LATERAL (
    SELECT jsonb_agg(DISTINCT
                     jsonb_build_object('name', h.name,
                                        'color', h.color)
           )::json AS hashtags
    FROM hashtag_topic ht
             JOIN hashtag h ON h.id = ht.hashtag_id
    WHERE ht.topic_id = ct.id
    ) hs ON TRUE;

DISCARD PLANS;




SELECT t.relname          AS table_name,
       i.relname          AS index_name,
       array_agg(a.attname
                 ORDER BY s.ordinality) AS column_names
FROM   pg_class        t                      -- 테이블
           JOIN   pg_index        ix ON t.oid = ix.indrelid
           JOIN   pg_class        i  ON i.oid = ix.indexrelid   -- 인덱스
           JOIN   LATERAL unnest(ix.indkey)
    WITH ORDINALITY AS s(attnum, ordinality) ON TRUE
           JOIN   pg_attribute    a  ON a.attrelid = t.oid
    AND a.attnum   = s.attnum
WHERE  t.relkind = 'r'                 -- 일반 테이블만
  AND  t.relname IN ('topic','category','category_topic',
                     'hashtag_topic','hashtag')      -- 필요 테이블만
GROUP  BY t.relname, i.relname
ORDER  BY t.relname, i.relname;





EXPLAIN ANALYSE
SELECT id, storyboard_id, member_id, video_url, created_at, thumbnail_url, running_time, title FROM video WHERE member_id = '73470cbc-b383-4d92-8d74-fd68391de431' ORDER BY created_at DESC LIMIT 100 OFFSET 0;
DISCARD PLANS;