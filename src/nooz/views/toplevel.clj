(ns nooz.views.toplevel
  (:require [nooz.time :as nt]
            [nooz.views.common :as common]
            [nooz.views.item :as item]
            [nooz.models.post :as post])
  (:use [noir.core :only [defpage]]))

(defpage "/" {}
  (let [time (nt/long-now)
        posts (post/get-ranked-posts)]
    (common/layout (item/post-list time posts))))

(defpage "/latest" {}
  (let [time (nt/long-now)
        posts (post/get-latest-posts)]
    (common/layout (item/post-list time posts))))


