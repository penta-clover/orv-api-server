BEGIN;

INSERT INTO public.storyboard (id, title, start_scene_id)
VALUES ('C2D8A9C7-293F-4215-A717-E0C9BECD6D9B',
        '타입캡슐 (1)',
        null);

INSERT INTO public.storyboard_preview (storyboard_id, examples)
VALUES ('C2D8A9C7-293F-4215-A717-E0C9BECD6D9B',
        ARRAY[
            '지금의 나에게 가장 소중한 것은 무엇인가요?'
            ]);


INSERT INTO public.topic (id, name, description, thumbnail_url)
VALUES ('BED8A9C7-293F-4215-A717-E0C9BECD6D9B',
        '타입캡슐',
        '미래의 나에게 짧은 인사를 건네보아요',
        'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png');


INSERT INTO public.storyboard_topic (storyboard_id, topic_id)
VALUES ('C2D8A9C7-293F-4215-A717-E0C9BECD6D9B',
        'BED8A9C7-293F-4215-A717-E0C9BECD6D9B');

END;