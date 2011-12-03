(ns nooz.views.toplevel
  (:require [nooz.time :as nt]
            [nooz.views.common :as common]
            [nooz.views.item :as item]
            [nooz.models.post :as post])
  (:use [noir.core :only [defpage]]))

(defpage "/" {}
  (common/layout (item/post-list (nt/as-long (nt/now))
                                 (post/get-latest-posts))))

(defpage "/latest" {}
  (common/layout (item/post-list (nt/as-long (nt/now))
                                 (post/get-latest-posts))))


